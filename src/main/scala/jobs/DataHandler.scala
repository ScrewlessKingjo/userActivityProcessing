package jobs

import config.ConfigLoader
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory
import utils.SlackNotifier

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object DataHandler {
  private val logger = LoggerFactory.getLogger(getClass)
  private val maxRetries = 3
  private val retryDelay = 5000

  def run(spark: SparkSession): Unit = {
    val inputPath = ConfigLoader.AppConfig.inputPath
    val outputPath = ConfigLoader.AppConfig.outputPath
    val compression = ConfigLoader.AppConfig.compression
    val checkpointPath = outputPath + "/_SUCCESS"
    val hiveDB = ConfigLoader.HiveConfig.database
    val hiveTable = ConfigLoader.HiveConfig.table
    val hiveLocation = ConfigLoader.HiveConfig.location

    // 완료된 배치를 확인하는 함수
    def isJobCompleted: Boolean = Files.exists(Paths.get(checkpointPath))

    // 재시도 로직을 포함한 실행 함수
    def executeWithRetries[T](block: => T): T = {
      var retryCount = 0
      var lastError: Throwable = null

      while (retryCount < maxRetries) {
        Try(block) match {
          case Success(result) => return result
          case Failure(e) =>
            logger.error(s"Attempt ${retryCount + 1} failed: ${e.getMessage}")
            logger.error("Error in DataHandler", e)
            lastError = e
            retryCount += 1
            Thread.sleep(retryDelay)
        }
      }
      throw new Exception(s"Failed after $maxRetries attempts", lastError)
    }

    try {
      if (isJobCompleted) {
        logger.info("Job already completed. Skipping this run.")
        return
      }

      executeWithRetries {
        val csvFiles = Files.list(Paths.get(inputPath))
          .iterator()
          .asScala
          .filter(path => path.toString.endsWith(".csv"))
          .map(_.toString)
          .toArray

        val userActivityDF = spark.read
          .option("header", "true")
          .option("inferSchema", "true")
          .csv(csvFiles: _*)


        val processedDF = userActivityDF
          .withColumn("timestamp", unix_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
          .withColumn("timestamp_as_ts", from_unixtime(col("timestamp")))
          .withColumn("timestamp_kst", from_utc_timestamp(col("timestamp_as_ts"), "Asia/Seoul"))
          .withColumn("date", to_date(col("timestamp_kst")))
          .withColumn("date_partition", date_format(col("date"), "yyyy/MM/dd"))

        val optimizedDF = processedDF.select("timestamp_kst", "event_type", "product_id", "category_id", "category_code", "brand", "price", "user_id", "user_session", "date_partition")

        optimizedDF.write
          .mode(SaveMode.Overwrite)
          .partitionBy("date_partition")
          .option("compression", compression)
          .parquet(outputPath)
      }


      executeWithRetries {
        val create_db_query = s"CREATE DATABASE IF NOT EXISTS ${hiveDB}"
        spark.sql(create_db_query)

        val create_query =
          s"""
             CREATE EXTERNAL TABLE IF NOT EXISTS ${hiveDB}.${hiveTable} (
                event_time STRING,
                event_type STRING,
                product_id STRING,
                category_id STRING,
                category_code STRING,
                brand STRING,
                price DOUBLE,
                user_id STRING,
                user_session STRING
             )
        PARTITIONED BY (date_partition STRING)
        LOCATION '${hiveLocation}'
           """
        spark.sql(create_query)

        val repair_query = s"MSCK REPAIR TABLE ${hiveDB}.${hiveTable}"
        spark.sql(repair_query)
      }

      Files.write(Paths.get(checkpointPath), Array.emptyByteArray, StandardOpenOption.CREATE)

      logger.info("Data processing completed successfully.")

    } catch {
      case e: Exception =>
        logger.error("Error in DataHandler", e)
        val errorMessage = s"${e.getClass.getName}: ${e.getMessage}"
        SlackNotifier.sendErrorNotification(errorMessage)  // Slack 알림 전송
        throw e
    }
  }
}
