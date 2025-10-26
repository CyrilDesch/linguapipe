package linguapipe.infrastructure.adapters.driven.transcriptor

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.TranscriberPort
import linguapipe.domain.{HealthStatus, IngestPayload, IngestSource, Transcript, TranscriptId, TranscriptMetadata}

/**
 * Transcription service adapter supporting multiple providers (AssemblyAI,
 * Whisper).
 */
final class TranscriptionService(provider: String = "mock") extends TranscriberPort {

  override def transcribe(payload: IngestPayload): Task[Transcript] =
    ZIO.succeed {
      val transcriptId = TranscriptId(java.util.UUID.randomUUID().toString)
      val now          = Instant.now()

      Transcript(
        id = transcriptId,
        language = "en",       // Mock language detection
        segments = List.empty, // Mock empty segments for now
        createdAt = now,
        metadata = TranscriptMetadata(
          source = IngestSource.Audio, // Mock source
          attributes = Map("provider" -> provider)
        )
      )
    }

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      // In a real implementation, this would test the transcription service connection
      val isHealthy = true // This would be replaced with actual connection test

      if (isHealthy) {
        HealthStatus.Healthy(
          serviceName = s"Transcriber($provider)",
          checkedAt = Instant.now(),
          details = Map(
            "provider" -> provider
          )
        )
      } else {
        HealthStatus.Unhealthy(
          serviceName = s"Transcriber($provider)",
          checkedAt = Instant.now(),
          error = "Connection failed",
          details = Map(
            "provider" -> provider
          )
        )
      }
    }
}
