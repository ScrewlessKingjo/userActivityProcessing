import jobs.DataHandler
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

object AppMain {
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = initializeSparkSession()

    try {
      logger.info("Start DataHandler")
      DataHandler.run(spark)
      logger.info("DataHandler finished successfully.")
    } catch {
      case e: Exception =>
        logger.error("Error running DataHandler", e)
        throw e
    } finally {
      spark.stop()
    }
  }

  private def initializeSparkSession(): SparkSession = {
    SparkSession.builder()
      .appName("Spark Application")
      .master("local[*]")
      .config("spark.testing.memory", "34359738368")
      .config("spark.driver.memory", "32g")
      .config("spark.executor.memory", "4g")
      .config("parquet.block.size", 134217728) // 128MB
      .config("spark.sql.shuffle.partitions", "400")
      .config("spark.sql.files.maxRecordsPerFile", "100000")
      .enableHiveSupport()
      .getOrCreate()
  }
}
