package linguapipe.infrastructure.adapters.driven.transcriptor

import java.time.Instant
import java.util.UUID

import zio.*

import linguapipe.application.ports.driven.TranscriberPort
import linguapipe.domain.{HealthStatus, IngestPayload, IngestSource, Transcript, TranscriptMetadata}

final class TranscriptionService(provider: String = "mock") extends TranscriberPort {

  override def transcribe(payload: IngestPayload): Task[Transcript] =
    ZIO.succeed {
      val transcriptId = UUID.randomUUID()
      val now          = Instant.now()

      Transcript(
        id = transcriptId,
        language = "en",
        segments = List.empty,
        createdAt = now,
        metadata = TranscriptMetadata(
          source = IngestSource.Audio,
          attributes = Map("provider" -> provider)
        )
      )
    }

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      val isHealthy = true

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
