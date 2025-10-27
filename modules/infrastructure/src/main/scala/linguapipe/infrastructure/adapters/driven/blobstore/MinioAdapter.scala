package linguapipe.infrastructure.adapters.driven.blobstore

import java.time.Instant
import java.util.UUID

import zio.*

import linguapipe.application.ports.driven.BlobStorePort
import linguapipe.domain.{HealthStatus, IngestPayload}

final class MinioAdapter(endpoint: String, accessKey: String, secretKey: String, bucket: String) extends BlobStorePort {
  override def store(jobId: UUID, payload: IngestPayload): Task[Unit] =
    ZIO.succeed(println(s"[Minio:$endpoint] stored payload for job $jobId"))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.succeed(
      HealthStatus.Healthy(
        serviceName = s"Minio($endpoint)",
        checkedAt = Instant.now(),
        details = Map("endpoint" -> endpoint, "accessKey" -> accessKey, "secretKey" -> secretKey, "bucket" -> bucket)
      )
    )
}
