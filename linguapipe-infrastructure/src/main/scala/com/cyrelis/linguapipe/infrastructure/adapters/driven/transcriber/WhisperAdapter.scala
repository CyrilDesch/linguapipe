package com.cyrelis.linguapipe.infrastructure.adapters.driven.transcriber

import java.net.http.HttpClient
import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.*

import com.cyrelis.linguapipe.application.ports.driven.TranscriberPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.{IngestSource, Transcript, TranscriptMetadata}
import com.cyrelis.linguapipe.infrastructure.config.TranscriberAdapterConfig
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.MediaType
import zio.*
import zio.json.*

final case class WhisperResponse(
  text: String
)

object WhisperResponse {
  given JsonDecoder[WhisperResponse] = DeriveJsonDecoder.gen[WhisperResponse]
  given JsonEncoder[WhisperResponse] = DeriveJsonEncoder.gen[WhisperResponse]
}

class WhisperAdapter(config: TranscriberAdapterConfig.Whisper) extends TranscriberPort {

  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .build()

  override def transcribe(audioContent: Array[Byte], format: String): Task[Transcript] = {
    val transcriptId = UUID.randomUUID()
    val now          = Instant.now()

    for {
      response        <- makeWhisperRequest(audioContent, format)
      whisperResponse <- parseWhisperResponse(response, audioContent.length, format)
      transcript      <- ZIO.succeed(
                      Transcript(
                        id = transcriptId,
                        text = whisperResponse.text,
                        createdAt = now,
                        metadata = TranscriptMetadata(
                          source = IngestSource.Audio,
                          attributes = Map(
                            "provider" -> "whisper",
                            "model"    -> config.modelPath,
                            "api_url"  -> config.apiUrl
                          )
                        )
                      )
                    )
    } yield transcript
  }

  protected def makeWhisperRequest(
    audioBytes: Array[Byte],
    format: String
  ): Task[Response[String]] = {
    val url = uri"${config.apiUrl}/asr".addParams(
      "encode"          -> "true",
      "task"            -> "transcribe",
      "vad_filter"      -> "false",
      "word_timestamps" -> "false",
      "output"          -> "json"
    )

    val request = basicRequest
      .post(url)
      .multipartBody(
        multipart("audio_file", audioBytes)
          .fileName(s"audio.${format.stripPrefix("x-")}")
          .contentType(MediaType.ApplicationOctetStream)
      )
      .response(asStringAlways)

    ZIO.scoped {
      for {
        backend  <- HttpClientZioBackend.scopedUsingClient(httpClient)
        response <- request.send(backend)
      } yield response
    }
  }

  private def parseWhisperResponse(
    response: Response[String],
    audioSize: Int,
    format: String
  ): Task[WhisperResponse] =
    if (response.code.isSuccess) {
      response.body.fromJson[WhisperResponse] match {
        case Right(whisperResponse) =>
          ZIO.succeed(whisperResponse)
        case Left(error) =>
          ZIO.fail(
            new RuntimeException(
              s"Invalid JSON from Whisper API (status ${response.code.code}): $error"
            )
          )
      }
    } else {
      ZIO.fail(
        new RuntimeException(
          s"Whisper API error (status ${response.code.code}): ${response.body} | Audio: $audioSize bytes, format=$format"
        )
      )
    }

  private def baseDetails: Map[String, String] = Map(
    "provider" -> "whisper",
    "model"    -> config.modelPath,
    "api_url"  -> config.apiUrl
  )

  override def healthCheck(): Task[HealthStatus] = {
    val serviceName = s"WhisperTranscriber(${config.modelPath})"
    val now         = Instant.now()

    val check = for {
      backend  <- HttpClientZioBackend()
      response <- basicRequest
                    .get(uri"${config.apiUrl}/docs")
                    .readTimeout(5.seconds)
                    .send(backend)
      _ <- backend.close()
    } yield {
      if (response.code.isSuccess) {
        HealthStatus.Healthy(
          serviceName = serviceName,
          checkedAt = now,
          details = baseDetails + ("status" -> "connected")
        )
      } else {
        HealthStatus.Unhealthy(
          serviceName = serviceName,
          checkedAt = now,
          error = s"HTTP ${response.code.code}",
          details = baseDetails + ("response_code" -> response.code.code.toString)
        )
      }
    }

    check.catchAll { error =>
      ZIO.succeed(error match {
        case _: java.net.ConnectException =>
          HealthStatus.Unhealthy(
            serviceName = serviceName,
            checkedAt = now,
            error = s"Connection failed: ${error.getMessage}",
            details = baseDetails + ("error_type" -> "connection")
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
            details = baseDetails + ("error_type" -> "unexpected")
          )
      })
    }
  }
}
