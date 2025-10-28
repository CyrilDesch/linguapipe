package linguapipe.infrastructure.adapters.driven.transcriber

import zio.*
import zio.test.*
import zio.test.Assertion.*

import linguapipe.domain.HealthStatus
import linguapipe.infrastructure.config.TranscriberAdapterConfig

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
      format: String,
      language: Option[String]
    ): Task[WhisperResponse] = stubbedResponse
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
      test("should reject invalid base64 content") {
        val invalidContent = "invalid-base64!@#$%"
        val adapter        = WhisperAdapter(testConfig)
        val result         = adapter.transcribe(invalidContent, "wav", Some("fr"))

        assertZIO(result.exit)(fails(anything))
      },
      test("should reject empty audio content") {
        val emptyContent = ""
        val adapter      = WhisperAdapter(testConfig)
        val result       = adapter.transcribe(emptyContent, "wav", Some("fr"))

        assertZIO(result.exit)(fails(anything))
      }
    ),
    suite("transcription scenarios")(
      test("should successfully transcribe audio and return transcript") {
        val mockResponse = WhisperResponse(
          text = "Bonjour, ceci est un test de transcription.",
          language = "fr",
          duration = 3.5
        )
        val adapter = new WhisperAdapterTestDouble(testConfig, ZIO.succeed(mockResponse))

        val validBase64 = java.util.Base64.getEncoder.encodeToString("fake audio data".getBytes)

        for {
          transcript <- adapter.transcribe(validBase64, "wav", Some("fr"))
        } yield assertTrue(
          transcript.text == "Bonjour, ceci est un test de transcription." &&
            transcript.language == "fr" &&
            transcript.metadata.attributes("provider") == "whisper" &&
            transcript.metadata.attributes("model") == "whisper-1" &&
            transcript.metadata.attributes("duration") == "3.5"
        )
      },
      test("should include correct metadata in transcript") {
        val mockResponse = WhisperResponse(
          text = "Test transcript",
          language = "en",
          duration = 2.0
        )
        val adapter = new WhisperAdapterTestDouble(testConfig, ZIO.succeed(mockResponse))

        val validBase64 = java.util.Base64.getEncoder.encodeToString("test audio".getBytes)

        for {
          transcript <- adapter.transcribe(validBase64, "mp3", Some("en"))
        } yield assertTrue(
          transcript.metadata.attributes.contains("provider") &&
            transcript.metadata.attributes.contains("model") &&
            transcript.metadata.attributes.contains("api_url") &&
            transcript.metadata.attributes.contains("duration") &&
            transcript.metadata.attributes("api_url") == "http://localhost:8000"
        )
      },
      test("should propagate transcription errors") {
        val errorMessage = "Transcription service unavailable"
        val adapter      = new WhisperAdapterTestDouble(
          testConfig,
          ZIO.fail(new RuntimeException(errorMessage))
        )

        val validBase64 = java.util.Base64.getEncoder.encodeToString("audio data".getBytes)

        assertZIO(adapter.transcribe(validBase64, "wav", Some("en")).exit)(
          fails(isSubtype[RuntimeException](hasMessage(equalTo(errorMessage))))
        )
      },
      test("should handle different audio formats") {
        val mockResponse = WhisperResponse(
          text = "Format test",
          language = "en",
          duration = 1.0
        )
        val adapter = new WhisperAdapterTestDouble(testConfig, ZIO.succeed(mockResponse))

        val validBase64 = java.util.Base64.getEncoder.encodeToString("audio".getBytes)
        val formats     = Seq("wav", "mp3", "m4a", "ogg")

        for {
          transcripts <- ZIO.foreach(formats) { format =>
                           adapter.transcribe(validBase64, format, Some("en"))
                         }
        } yield assertTrue(transcripts.forall(_.text == "Format test"))
      }
    )
  )
}
