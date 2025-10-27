package linguapipe.infrastructure.adapters.driven.transcriber

import java.net.URI
import java.time.Instant
import java.util.{Base64, UUID}

import zio.*
import zio.json.*

import linguapipe.application.ports.driven.TranscriberPort
import linguapipe.domain.{HealthStatus, IngestPayload, IngestSource, Transcript, TranscriptMetadata}
import linguapipe.infrastructure.config.TranscriberAdapterConfig

// Whisper API response models
final case class WhisperSegment(
  id: Int,
  seek: Double,
  start: Double,
  end: Double,
  text: String,
  tokens: List[Int],
  temperature: Double,
  avg_logprob: Double,
  compression_ratio: Double,
  no_speech_prob: Double
)

final case class WhisperResponse(
  text: String,
  language: String,
  duration: Double,
  segments: List[WhisperSegment]
)

object WhisperSegment {
  given JsonDecoder[WhisperSegment] = DeriveJsonDecoder.gen[WhisperSegment]
}

object WhisperResponse {
  given JsonDecoder[WhisperResponse] = DeriveJsonDecoder.gen[WhisperResponse]
}

final class WhisperAdapter(config: TranscriberAdapterConfig.Whisper) extends TranscriberPort {

  override def transcribe(payload: IngestPayload.Base64Audio): Task[Transcript] =
    transcribeAudio(payload.content, payload.format, payload.language)

  private def transcribeAudio(base64Content: String, format: String, language: Option[String]): Task[Transcript] = {
    val transcriptId = UUID.randomUUID()
    val now          = Instant.now()

    for {
      // Decode base64 content to bytes
      audioBytes <- ZIO.attempt(Base64.getDecoder.decode(base64Content))

      // Create multipart form data for the Whisper API
      formData = createMultipartFormData(audioBytes, format, language)

      // Make HTTP request to Whisper service
      response <- makeWhisperRequest(formData)

      // Parse response and create transcript
      transcript <- parseWhisperResponse(response, transcriptId, now)
    } yield transcript
  }

  private def createMultipartFormData(audioBytes: Array[Byte], format: String, language: Option[String]): String = {
    val boundary    = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    val contentType = s"audio/$format"

    val languageParam = language
      .map(lang => s"--$boundary\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\n$lang\r\n")
      .getOrElse("")

    s"""--$boundary
Content-Disposition: form-data; name="file"; filename="audio.$format"
Content-Type: $contentType

${new String(audioBytes)}
--$boundary
Content-Disposition: form-data; name="response_format"

json
$languageParam--$boundary--"""
  }

  private def makeWhisperRequest(formData: String): Task[String] =
    ZIO.attempt {
      val url        = s"${config.apiUrl}/asr"
      val connection = URI.create(url).toURL.openConnection().asInstanceOf[java.net.HttpURLConnection]

      try {
        connection.setRequestMethod("POST")
        connection.setRequestProperty(
          "Content-Type",
          s"multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"
        )
        connection.setDoOutput(true)
        connection.setConnectTimeout(30000) // 30 seconds
        connection.setReadTimeout(120000)   // 2 minutes

        // Write form data
        val outputStream = connection.getOutputStream
        outputStream.write(formData.getBytes("UTF-8"))
        outputStream.close()

        // Read response
        val responseCode = connection.getResponseCode
        if (responseCode >= 200 && responseCode < 300) {
          val inputStream = connection.getInputStream
          val response    = scala.io.Source.fromInputStream(inputStream).mkString
          inputStream.close()
          response
        } else {
          val errorStream   = connection.getErrorStream
          val errorResponse = if (errorStream != null) {
            scala.io.Source.fromInputStream(errorStream).mkString
          } else "Unknown error"
          if (errorStream != null) errorStream.close()
          throw new RuntimeException(s"Whisper API error ($responseCode): $errorResponse")
        }
      } finally {
        connection.disconnect()
      }
    }

  private def parseWhisperResponse(response: String, transcriptId: UUID, createdAt: Instant): Task[Transcript] =
    ZIO.attempt {
      // Parse Whisper JSON response using zio-json
      val whisperResponse = response.fromJson[WhisperResponse].getOrElse {
        throw new RuntimeException(s"Failed to parse Whisper response: $response")
      }

      Transcript(
        id = transcriptId,
        language = whisperResponse.language,
        text = whisperResponse.text,
        createdAt = createdAt,
        metadata = TranscriptMetadata(
          source = IngestSource.Audio,
          attributes = Map(
            "provider" -> "whisper",
            "model"    -> config.modelPath,
            "api_url"  -> config.apiUrl,
            "duration" -> whisperResponse.duration.toString
          )
        )
      )
    }

  override def healthCheck(): Task[HealthStatus] = {
    val serviceName = s"WhisperTranscriber(${config.modelPath})"
    val now         = Instant.now()

    ZIO.attempt {
      val url        = s"${config.apiUrl}/docs"
      val connection = URI.create(url).toURL.openConnection().asInstanceOf[java.net.HttpURLConnection]

      try {
        connection.setRequestMethod("GET")
        connection.setConnectTimeout(5000) // 5 seconds
        connection.setReadTimeout(5000)    // 5 seconds

        val responseCode = connection.getResponseCode
        if (responseCode >= 200 && responseCode < 300) {
          HealthStatus.Healthy(
            serviceName = serviceName,
            checkedAt = now,
            details = Map(
              "provider" -> "whisper",
              "model"    -> config.modelPath,
              "api_url"  -> config.apiUrl,
              "status"   -> "connected"
            )
          )
        } else {
          HealthStatus.Unhealthy(
            serviceName = serviceName,
            checkedAt = now,
            error = s"HTTP $responseCode",
            details = Map(
              "provider"      -> "whisper",
              "model"         -> config.modelPath,
              "api_url"       -> config.apiUrl,
              "response_code" -> responseCode.toString
            )
          )
        }
      } catch {
        case e: java.net.ConnectException =>
          HealthStatus.Unhealthy(
            serviceName = serviceName,
            checkedAt = now,
            error = s"Connection failed: ${e.getMessage}",
            details = Map(
              "provider"   -> "whisper",
              "model"      -> config.modelPath,
              "api_url"    -> config.apiUrl,
              "error_type" -> "connection"
            )
          )
        case _: java.net.SocketTimeoutException =>
          HealthStatus.Timeout(
            serviceName = serviceName,
            checkedAt = now,
            timeoutMs = 5000
          )
        case e: Exception =>
          HealthStatus.Unhealthy(
            serviceName = serviceName,
            checkedAt = now,
            error = s"Unexpected error: ${e.getMessage}",
            details = Map(
              "provider"   -> "whisper",
              "model"      -> config.modelPath,
              "api_url"    -> config.apiUrl,
              "error_type" -> "unexpected"
            )
          )
      } finally {
        connection.disconnect()
      }
    }
  }
}
