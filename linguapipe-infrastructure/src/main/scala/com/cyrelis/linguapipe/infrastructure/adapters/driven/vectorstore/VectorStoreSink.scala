package com.cyrelis.linguapipe.infrastructure.adapters.driven.vectorstore

import java.time.Instant
import java.util.UUID

import zio.*

import com.cyrelis.linguapipe.application.ports.driven.VectorSinkPort
import com.cyrelis.linguapipe.application.types.HealthStatus

final class VectorStoreSink(provider: String = "inmemory") extends VectorSinkPort {
  override def upsertEmbeddings(
    transcriptId: UUID,
    vectors: List[Array[Float]]
  ): Task[Unit] =
    ZIO.succeed(println(s"[VectorStore:$provider] Upsert ${vectors.size} vectors for $transcriptId"))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      val isHealthy = true

      if (isHealthy) {
        HealthStatus.Healthy(
          serviceName = s"VectorStore($provider)",
          checkedAt = Instant.now(),
          details = Map(
            "provider" -> provider
          )
        )
      } else {
        HealthStatus.Unhealthy(
          serviceName = s"VectorStore($provider)",
          checkedAt = Instant.now(),
          error = "Connection failed",
          details = Map(
            "provider" -> provider
          )
        )
      }
    }
}
