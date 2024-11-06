package config

import com.typesafe.config.{Config, ConfigFactory}

object ConfigLoader {
  private val config: Config = ConfigFactory.load()

  object AppConfig {
    val inputPath: String = config.getString("app.input.path")
    val outputPath: String = config.getString("app.output.path")
    val compression: String = config.getString("app.output.compression")
  }

  object HiveConfig {
    val database: String = config.getString("app.hive.database")
    val table: String = config.getString("app.hive.table")
    val location: String = config.getString("app.hive.location")
  }

  object PartitionConfig {
    val dateFormat: String = config.getString("app.partition.date_format")
  }

  object ScheduleConfig {
    val intervalMinutes: Int = config.getInt("app.schedule.interval_minutes")
  }

  object LogConfig {
    val logPath: String = config.getString("app.logs.path")
  }

  object SlackConfig {
    val webhookUrl: String = config.getString("slack.webhookUrl")
  }
}
