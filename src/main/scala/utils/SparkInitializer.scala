package utils

import org.apache.spark.sql.SparkSession

object SparkInitializer {
  private val driverMemory = "32g"
  private val executorMemory = "4g"

  def initializeSparkSession(): SparkSession = {
    SparkSession.builder()
      .appName("Spark Application")
      .master("local[*]")
      .config("spark.driver.memory", driverMemory)
      .config("spark.executor.memory", executorMemory)
      .config("parquet.block.size", 134217728)
      .config("spark.sql.shuffle.partitions", "400")
      .config("spark.sql.files.maxRecordsPerFile", "100000")
      .enableHiveSupport()
      .getOrCreate()
  }
}
