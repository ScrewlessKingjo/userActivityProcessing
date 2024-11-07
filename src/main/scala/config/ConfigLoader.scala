package config

import com.typesafe.config.{Config, ConfigFactory}

// config 객체화 모듈
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

  object SlackConfig {
    val webhookUrl: String = config.getString("app.slack.webhookUrl")
  }
}
