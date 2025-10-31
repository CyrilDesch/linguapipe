package com.cyrelis.linguapipe.infrastructure.migration

import com.cyrelis.linguapipe.infrastructure.config.BlobStoreAdapterConfig
import io.minio.*
import io.minio.errors.ErrorResponseException
import zio.*

trait BlobStoreInitializer {
  def initialize(): Task[Unit]
}

object BlobStoreInitializer {

  final class MinIOInitializer(config: BlobStoreAdapterConfig.MinIO) extends BlobStoreInitializer {

    override def initialize(): Task[Unit] =
      ZIO.logInfo(s"Initializing MinIO bucket: ${config.bucket}") *>
        createClient().flatMap { client =>
          createBucketIfNotExists(client) *>
            ZIO.logInfo(s"MinIO bucket '${config.bucket}' is ready")
        }

    private def createClient(): Task[MinioClient] =
      ZIO.attempt {
        MinioClient
          .builder()
          .endpoint(config.host, config.port, false)
          .credentials(config.accessKey, config.secretKey)
          .build()
      }.catchSome { case e: IllegalArgumentException =>
        ZIO.fail(new IllegalArgumentException(s"Invalid MinIO configuration: ${e.getMessage}", e))
      }

    private def createBucketIfNotExists(client: MinioClient): Task[Unit] =
      ZIO.attemptBlocking {
        val exists = client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())
        if (!exists) {
          client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
        }
      }.catchSome { case e: ErrorResponseException =>
        ZIO.fail(new RuntimeException(s"Failed to create MinIO bucket '${config.bucket}': ${e.getMessage}", e))
      }
  }

  def layer: ZLayer[BlobStoreAdapterConfig, Nothing, BlobStoreInitializer] =
    ZLayer.fromFunction { (config: BlobStoreAdapterConfig) =>
      config match {
        case cfg: BlobStoreAdapterConfig.MinIO => new MinIOInitializer(cfg)
      }
    }
}
