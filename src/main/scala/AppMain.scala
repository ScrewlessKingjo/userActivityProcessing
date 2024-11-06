import jobs.DataHandler
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory
import utils.SparkInitializer

object AppMain {
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = SparkInitializer.initializeSparkSession()

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
}
