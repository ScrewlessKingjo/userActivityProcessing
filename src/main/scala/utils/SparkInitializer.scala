package utils
import org.apache.spark.sql.SparkSession
import config.ConfigLoader

// Spark 초기화 모듈
object SparkInitializer {
  private val mode = ConfigLoader.SparkConfig.mode
  private val driverMemory = ConfigLoader.SparkConfig.driverMemory
  private val executorMemory = ConfigLoader.SparkConfig.executorMemory
  private val shufflePartitions = ConfigLoader.SparkConfig.shufflePartitions
  private val parquetSize = ConfigLoader.SparkConfig.parquetSize
  private val maxRecordsPerFile = ConfigLoader.SparkConfig.maxRecordsPerFile

  def initializeSparkSession(): SparkSession = {
    SparkSession.builder()
      .appName("Spark Application")
      .master(mode)
      .config("spark.driver.memory", driverMemory)
      .config("spark.executor.memory", executorMemory)
      .config("parquet.block.size", parquetSize)
      .config("spark.sql.shuffle.partitions", shufflePartitions)
      .config("spark.sql.files.maxRecordsPerFile", maxRecordsPerFile)
      .enableHiveSupport()
      .getOrCreate()
  }
}
