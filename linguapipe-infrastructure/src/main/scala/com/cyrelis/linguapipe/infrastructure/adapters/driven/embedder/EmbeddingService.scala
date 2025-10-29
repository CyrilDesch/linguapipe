package com.cyrelis.linguapipe.infrastructure.adapters.driven.embedder

import java.time.Instant

import com.cyrelis.linguapipe.application.ports.driven.EmbedderPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.Transcript
import zio.*

final class EmbeddingService(provider: String = "mock") extends EmbedderPort {

  override def embed(transcript: Transcript): Task[Array[Float]] =
    ZIO.succeed(Array.fill(384)(0.1f))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      val isHealthy = true

      if (isHealthy) {
        HealthStatus.Healthy(
          serviceName = s"Embedder($provider)",
          checkedAt = Instant.now(),
          details = Map(
            "provider" -> provider
          )
        )
      } else {
        HealthStatus.Unhealthy(
          serviceName = s"Embedder($provider)",
          checkedAt = Instant.now(),
          error = "Connection failed",
          details = Map(
            "provider" -> provider
          )
        )
      }
    }
}
