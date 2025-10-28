package com.cyrelis.linguapipe.infrastructure.adapters.driven.transcriber

import java.time.Instant
import java.util.{Base64, UUID}

import scala.concurrent.duration.*

import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.client4.ziojson.*
import sttp.model.MediaType
import zio.*
import zio.json.*

import com.cyrelis.linguapipe.application.ports.driven.TranscriberPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.{IngestSource, Transcript, TranscriptMetadata}
import com.cyrelis.linguapipe.infrastructure.config.TranscriberAdapterConfig

final case class WhisperResponse(
  text: String,
  language: String,
  duration: Double
)

object WhisperResponse {
  given JsonDecoder[WhisperResponse] = DeriveJsonDecoder.gen[WhisperResponse]
}

class WhisperAdapter(config: TranscriberAdapterConfig.Whisper) extends TranscriberPort {

  override def transcribe(audioContent: String, format: String, language: Option[String]): Task[Transcript] = {
    val transcriptId = UUID.randomUUID()
    val now          = Instant.now()

    for {
      audioBytes <- ZIO.attempt(Base64.getDecoder.decode(audioContent))
      response   <- makeWhisperRequest(audioBytes, format, language)
      transcript <- parseWhisperResponse(response, transcriptId, now)
    } yield transcript
  }

  protected def makeWhisperRequest(
    audioBytes: Array[Byte],
    format: String,
    language: Option[String]
  ): Task[WhisperResponse] = {
    val url = uri"${config.apiUrl}/asr"

    val baseParts = Seq(
      multipart("file", audioBytes)
        .fileName(s"audio.$format")
        .contentType(MediaType.unsafeParse(s"audio/$format")),
      multipart("response_format", "json")
    )

    val allParts = language match {
      case Some(lang) => baseParts :+ multipart("language", lang)
      case None       => baseParts
    }

    val request = basicRequest
      .post(url)
      .multipartBody(allParts)
      .readTimeout(2.minutes)
      .response(asJson[WhisperResponse])

    for {
      backend  <- HttpClientZioBackend()
      response <- request.send(backend)
      _        <- backend.close()
      result   <- response.body match {
                  case Right(whisperResponse) => ZIO.succeed(whisperResponse)
                  case Left(error)            =>
                    ZIO.fail(
                      new RuntimeException(s"Whisper API error: ${error.getMessage}")
                    )
                }
    } yield result
  }

  private def parseWhisperResponse(
    response: WhisperResponse,
    transcriptId: UUID,
    createdAt: Instant
  ): Task[Transcript] =
    ZIO.succeed(
      Transcript(
        id = transcriptId,
        language = response.language,
        text = response.text,
        createdAt = createdAt,
        metadata = TranscriptMetadata(
          source = IngestSource.Audio,
          attributes = Map(
            "provider" -> "whisper",
            "model"    -> config.modelPath,
            "api_url"  -> config.apiUrl,
            "duration" -> response.duration.toString
          )
        )
      )
    )

  override def healthCheck(): Task[HealthStatus] = {
    val serviceName = s"WhisperTranscriber(${config.modelPath})"
    val now         = Instant.now()

    (for {
      backend  <- HttpClientZioBackend()
      response <- basicRequest
                    .get(uri"${config.apiUrl}/docs")
                    .readTimeout(5.seconds)
                    .send(backend)
      _ <- backend.close()
    } yield {
      val code = response.code.code
      if (code >= 200 && code < 300) {
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
          error = s"HTTP $code",
          details = Map(
            "provider"      -> "whisper",
            "model"         -> config.modelPath,
            "api_url"       -> config.apiUrl,
            "response_code" -> code.toString
          )
        )
      }
    }).catchAll { error =>
      ZIO.succeed(
        error match {
          case _: java.net.ConnectException =>
            HealthStatus.Unhealthy(
              serviceName = serviceName,
              checkedAt = now,
              error = s"Connection failed: ${error.getMessage}",
              details = Map(
                "provider"   -> "whisper",
                "model"      -> config.modelPath,
                "api_url"    -> config.apiUrl,
                "error_type" -> "connection"
              )
            )
          case _: sttp.client4.SttpClientException.TimeoutException =>
            HealthStatus.Timeout(
              serviceName = serviceName,
              checkedAt = now,
              timeoutMs = 5000
            )
          case _ =>
            HealthStatus.Unhealthy(
              serviceName = serviceName,
              checkedAt = now,
              error = s"Unexpected error: ${error.getMessage}",
              details = Map(
                "provider"   -> "whisper",
                "model"      -> config.modelPath,
                "api_url"    -> config.apiUrl,
                "error_type" -> "unexpected"
              )
            )
        }
      )
    }
  }
}
