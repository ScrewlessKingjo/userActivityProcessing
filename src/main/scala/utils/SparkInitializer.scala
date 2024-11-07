package utils

import org.apache.spark.sql.SparkSession

// Spark 초기화 모듈
object SparkInitializer {
  private val mode = "local[*]"
  private val driverMemory = "32g"
  private val executorMemory = "4g"
  private val shufflePartitions = "400"
  private val parquetSize = 134217728
  private val maxRecordsPerFile = "100000"

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
