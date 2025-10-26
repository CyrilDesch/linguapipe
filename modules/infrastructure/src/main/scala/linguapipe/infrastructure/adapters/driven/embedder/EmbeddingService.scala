package linguapipe.infrastructure.adapters.driven.embedder

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.EmbedderPort
import linguapipe.domain.{HealthStatus, Segment}

/**
 * Embedding service adapter supporting multiple providers (OpenAI,
 * HuggingFace).
 */
final class EmbeddingService(provider: String = "mock") extends EmbedderPort {

  override def embed(segment: Segment): Task[Array[Float]] =
    ZIO.succeed(Array.fill(384)(0.1f)) // Mock embedding vector

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      // In a real implementation, this would test the embedding service connection
      val isHealthy = true // This would be replaced with actual connection test

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
