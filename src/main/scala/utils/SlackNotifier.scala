package utils

import config.ConfigLoader
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.slf4j.{Logger, LoggerFactory}

import java.net.{HttpURLConnection, URL}

// 오류 발생 시 Slack 메신저를 통한 오류내역 전송 모듈
object SlackNotifier {
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val webhookUrl = ConfigLoader.SlackConfig.webhookUrl

  def sendErrorNotification(errorMessage: String): Unit = {
    try {
       val jsonMessage = compact(render(
        ("text" -> s":x: *Spark Job Error Detected* :x:") ~
          ("attachments" -> List(
            ("color" -> "danger") ~
              ("fields" -> List(
                ("title" -> "Error Message") ~ ("value" -> errorMessage) ~ ("short" -> false)
              ))
          ))
      ))

      val url = new URL(webhookUrl)
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setDoOutput(true)

      val outputStream = connection.getOutputStream
      outputStream.write(jsonMessage.getBytes("UTF-8"))
      outputStream.flush()
      outputStream.close()

      val responseCode = connection.getResponseCode
      if (responseCode == 200) {
        logger.info("Slack notification sent successfully.")
      } else {
        logger.error(s"Failed to send Slack notification. Response code: $responseCode")
      }
    } catch {
      case e: Exception =>
        logger.error("Failed to send error notification to Slack", e)
    }
  }
}
