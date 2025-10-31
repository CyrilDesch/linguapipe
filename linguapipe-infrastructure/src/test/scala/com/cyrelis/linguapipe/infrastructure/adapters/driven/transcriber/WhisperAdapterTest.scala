package com.cyrelis.linguapipe.infrastructure.adapters.driven.transcriber

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.config.TranscriberAdapterConfig
import io.circe.syntax.*
import sttp.client4.*
import sttp.model.{Method, RequestMetadata, StatusCode}
import zio.*
import zio.test.*
import zio.test.Assertion.*

object WhisperAdapterTest extends ZIOSpecDefault {

  private val testConfig: TranscriberAdapterConfig.Whisper = TranscriberAdapterConfig.Whisper(
    modelPath = "whisper-1",
    apiUrl = "http://localhost:8000"
  )

  /** Mock */
  private class WhisperAdapterTestDouble(
    config: TranscriberAdapterConfig.Whisper,
    stubbedResponse: Task[WhisperResponse]
  ) extends WhisperAdapter(config) {
    override protected def makeWhisperRequest(
      audioBytes: Array[Byte],
      mediaContentType: String,
      mediaFilename: String
    ): Task[Response[String]] =
      stubbedResponse.map { whisperResponse =>
        Response(
          body = whisperResponse.asJson.noSpaces,
          code = StatusCode.Ok,
          requestMetadata = RequestMetadata(Method.POST, uri"http://localhost:8000/asr", Nil)
        )
      }
  }

  def spec = suite("WhisperAdapter")(
    suite("health check scenarios")(
      test("should return Unhealthy with connection error for unreachable server") {
        val invalidConfig: TranscriberAdapterConfig.Whisper = TranscriberAdapterConfig.Whisper(
          modelPath = "whisper-1",
          apiUrl = "http://nonexistent-server-xyz:9999"
        )
        val adapter = WhisperAdapter(invalidConfig)

        for {
          status <- adapter.healthCheck()
        } yield assertTrue(
          status match {
            case HealthStatus.Unhealthy(serviceName, _, _, _) =>
              serviceName.contains("WhisperTranscriber")
            case _ => false
          }
        )
      },
      test("should include error details in unhealthy status") {
        val invalidConfig: TranscriberAdapterConfig.Whisper = TranscriberAdapterConfig.Whisper(
          modelPath = "whisper-test",
          apiUrl = "http://localhost:99999"
        )
        val adapter = WhisperAdapter(invalidConfig)

        for {
          status <- adapter.healthCheck()
        } yield {
          status match {
            case HealthStatus.Unhealthy(_, _, _, details) =>
              assertTrue(
                details.contains("provider") &&
                  details("provider") == "whisper" &&
                  details.contains("model") &&
                  details("model") == "whisper-test"
              )
            case _ => assertTrue(false)
          }
        }
      },
      test("should set service name with model path") {
        val adapter = WhisperAdapter(testConfig)

        for {
          status <- adapter.healthCheck()
        } yield {
          val serviceName = status match {
            case HealthStatus.Healthy(name, _, _)      => name
            case HealthStatus.Unhealthy(name, _, _, _) => name
            case HealthStatus.Timeout(name, _, _)      => name
          }
          assertTrue(serviceName.contains("whisper-1"))
        }
      }
    ),
    suite("input validation")(
      test("should reject invalid audio content") {
        val invalidContent = "invalid-content".getBytes
        val adapter        = WhisperAdapter(testConfig)
        val result         = adapter.transcribe(invalidContent, "audio/wav", "test.wav")

        assertZIO(result.exit)(fails(anything))
      },
      test("should reject empty audio content") {
        val emptyContent = Array.emptyByteArray
        val adapter      = WhisperAdapter(testConfig)
        val result       = adapter.transcribe(emptyContent, "audio/wav", "test.wav")

        assertZIO(result.exit)(fails(anything))
      }
    ),
    suite("transcription scenarios")(
      test("should successfully transcribe audio and return transcript") {
        val mockResponse = WhisperResponse(
          text = "Bonjour, ceci est un test de transcription.",
          language = None
        )
        val adapter = new WhisperAdapterTestDouble(testConfig, ZIO.succeed(mockResponse))

        val audioBytes = "fake audio data".getBytes

        for {
          transcript <- adapter.transcribe(audioBytes, "audio/wav", "test.wav")
        } yield assertTrue(
          transcript.text == "Bonjour, ceci est un test de transcription." &&
            transcript.metadata("provider") == "whisper" &&
            transcript.metadata("model") == "whisper-1"
        )
      },
      test("should include correct metadata in transcript") {
        val mockResponse = WhisperResponse(
          text = "Test transcript",
          language = None
        )
        val adapter = new WhisperAdapterTestDouble(testConfig, ZIO.succeed(mockResponse))

        val audioBytes = "test audio".getBytes

        for {
          transcript <- adapter.transcribe(audioBytes, "audio/mpeg", "test.mp3")
        } yield assertTrue(
          transcript.metadata.contains("provider") &&
            transcript.metadata.contains("model")
        )
      },
      test("should propagate transcription errors") {
        val errorMessage = "Transcription service unavailable"
        val adapter      = new WhisperAdapterTestDouble(
          testConfig,
          ZIO.fail(new RuntimeException(errorMessage))
        )

        val audioBytes = "audio data".getBytes

        assertZIO(adapter.transcribe(audioBytes, "audio/wav", "test.wav").exit)(
          fails(isSubtype[PipelineError.TranscriptionError](hasField("message", _.message, equalTo(errorMessage))))
        )
      },
      test("should handle different audio content types") {
        val mockResponse = WhisperResponse(
          text = "Content type test",
          language = None
        )
        val adapter = new WhisperAdapterTestDouble(testConfig, ZIO.succeed(mockResponse))

        val audioBytes   = "audio".getBytes
        val contentTypes = Seq(
          ("audio/wav", "test.wav"),
          ("audio/mpeg", "test.mp3"),
          ("audio/mp4", "test.m4a"),
          ("audio/ogg", "test.ogg")
        )

        for {
          transcripts <- ZIO.foreach(contentTypes) { case (contentType, filename) =>
                           adapter.transcribe(audioBytes, contentType, filename)
                         }
        } yield assertTrue(transcripts.forall(_.text == "Content type test"))
      }
    )
  )
}
