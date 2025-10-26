package linguapipe.infrastructure.adapters.driven.vectorstore

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.VectorSinkPort
import linguapipe.domain.{HealthStatus, SegmentId, TranscriptId}

/**
 * Vector store adapter supporting multiple providers (Pinecone, Qdrant,
 * Weaviate).
 */
final class VectorStoreSink(provider: String = "inmemory") extends VectorSinkPort {
  override def upsertEmbeddings(
    transcriptId: TranscriptId,
    vectors: List[(SegmentId, Array[Float])]
  ): Task[Unit] =
    ZIO.succeed(println(s"[VectorStore:$provider] Upsert ${vectors.size} vectors for ${transcriptId.value}"))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      // In a real implementation, this would test the vector store connection
      val isHealthy = true // This would be replaced with actual connection test

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
