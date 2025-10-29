package com.cyrelis.linguapipe.infrastructure.adapters.driven.blobstore

import java.time.Instant
import java.util.UUID

import com.cyrelis.linguapipe.application.ports.driven.BlobStorePort
import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

final class MinioAdapter(endpoint: String, accessKey: String, secretKey: String, bucket: String) extends BlobStorePort {
  override def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): Task[Unit] =
    ZIO.succeed(println(s"[Minio:$endpoint] stored audio ($format, ${audioContent.length} bytes) for job $jobId"))

  override def storeDocument(jobId: UUID, documentContent: String, mediaType: String): Task[Unit] =
    ZIO.succeed(println(s"[Minio:$endpoint] stored document ($mediaType) for job $jobId"))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.succeed(
      HealthStatus.Healthy(
        serviceName = s"Minio($endpoint)",
        checkedAt = Instant.now(),
        details = Map("endpoint" -> endpoint, "accessKey" -> accessKey, "secretKey" -> secretKey, "bucket" -> bucket)
      )
    )
}
