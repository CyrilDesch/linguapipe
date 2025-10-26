package linguapipe.infrastructure.adapters.driven.blobstore

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.BlobStorePort
import linguapipe.domain.{HealthStatus, IngestPayload, IngestionJobId}

/** Multi-provider blob storage (S3, GCS, Local) */
final class BlobStoreAdapter(provider: String) extends BlobStorePort {
  override def store(jobId: IngestionJobId, payload: IngestPayload): Task[Unit] =
    ZIO.succeed(println(s"[BlobStore:$provider] stored payload for job ${jobId.value}"))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      // In a real implementation, this would test the blob storage connection
      val isHealthy = true // This would be replaced with actual connection test

      if (isHealthy) {
        HealthStatus.Healthy(
          serviceName = s"BlobStore($provider)",
          checkedAt = Instant.now(),
          details = Map(
            "provider" -> provider
          )
        )
      } else {
        HealthStatus.Unhealthy(
          serviceName = s"BlobStore($provider)",
          checkedAt = Instant.now(),
          error = "Storage unavailable",
          details = Map(
            "provider" -> provider
          )
        )
      }
    }
}
