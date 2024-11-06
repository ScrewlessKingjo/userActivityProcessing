package jobs

import config.ConfigLoader
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object DataHandler {
  private val logger = LoggerFactory.getLogger(getClass)
  private val maxRetries = 3 // 최대 재시도 횟수
  private val retryDelay = 5000 // 재시도 간격 (밀리초)

  def run(spark: SparkSession): Unit = {
    val inputPath = ConfigLoader.AppConfig.inputPath
    val outputPath = ConfigLoader.AppConfig.outputPath
    val compression = ConfigLoader.AppConfig.compression
    val checkpointPath = outputPath + "/_SUCCESS" // 완료 체크포인트

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
            Thread.sleep(retryDelay) // 재시도 전 딜레이
        }
      }
      throw new Exception(s"Failed after $maxRetries attempts", lastError)
    }

    try {
      if (isJobCompleted) {
        logger.info("Job already completed. Skipping this run.")
        return
      }

      // 데이터 읽기 및 처리
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

        // userActivityDF.printSchema()

//        val sampleUserActivityDF =  userActivityDF.sample(0.1)
//        val processedDF = sampleUserActivityDF
//          .withColumn("timestamp", unix_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
//          .withColumn("timestamp_as_ts", from_unixtime(col("timestamp")))
//          .withColumn("timestamp_kst", from_utc_timestamp(col("timestamp_as_ts"), "Asia/Seoul"))
//          .withColumn("date", to_date(col("timestamp_kst")))
//          .withColumn("date_partition", date_format(col("date"), "yyyy/MM/dd"))

        val processedDF = userActivityDF
          .withColumn("timestamp", unix_timestamp(col("event_time"), "yyyy-MM-dd HH:mm:ss"))
          .withColumn("timestamp_as_ts", from_unixtime(col("timestamp")))
          .withColumn("timestamp_kst", from_utc_timestamp(col("timestamp_as_ts"), "Asia/Seoul"))
          .withColumn("date", to_date(col("timestamp_kst")))
          .withColumn("date_partition", date_format(col("date"), "yyyy/MM/dd"))

        val optimizedDF = processedDF.select("timestamp_kst", "event_type", "product_id", "category_id", "category_code", "brand", "price", "user_id", "user_session", "date_partition")

//        optimizedDF.show(10)

        optimizedDF.write
          .mode(SaveMode.Overwrite)
          .partitionBy("date_partition")
          .option("compression", compression)
          .parquet(outputPath)
      }

      // Hive External Table 관리
      executeWithRetries {
        spark.sql(s"CREATE DATABASE IF NOT EXISTS ${ConfigLoader.HiveConfig.database}")

        spark.sql(s"""
    CREATE EXTERNAL TABLE IF NOT EXISTS ${ConfigLoader.HiveConfig.database}.${ConfigLoader.HiveConfig.table} (
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
    LOCATION '${ConfigLoader.HiveConfig.location}'
  """)

        spark.sql(s"MSCK REPAIR TABLE ${ConfigLoader.HiveConfig.database}.${ConfigLoader.HiveConfig.table}")
      }

      // 작업 성공 시 체크포인트 파일 생성
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