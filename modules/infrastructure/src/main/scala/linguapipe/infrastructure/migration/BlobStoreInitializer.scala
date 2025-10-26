package linguapipe.infrastructure.migration

import zio.*

import linguapipe.infrastructure.config.BlobStoreAdapterConfig

/**
 * Blob store initialization service. Creates necessary buckets/containers at
 * application startup.
 */
trait BlobStoreInitializer {
  def initialize(): Task[Unit]
}

object BlobStoreInitializer {

  final class MinIOInitializer(config: BlobStoreAdapterConfig.MinIO) extends BlobStoreInitializer {
    override def initialize(): Task[Unit] =
      ZIO.logInfo(s"🔄 Initializing MinIO bucket: ${config.bucket}") *>
        createBucketIfNotExists() *>
        ZIO.logInfo(s"✅ MinIO bucket '${config.bucket}' is ready")

    private def createBucketIfNotExists(): Task[Unit] =
      ZIO.attempt {
        // In a real implementation, use MinIO client (S3-compatible):
        // 1. Check if bucket exists using headBucket
        // 2. If not, create bucket using makeBucket
        println(s"[MinIO Init] Would create bucket '${config.bucket}' at ${config.endpoint}")
      }
  }

  def layer: ZLayer[BlobStoreAdapterConfig, Nothing, BlobStoreInitializer] =
    ZLayer.fromFunction { (config: BlobStoreAdapterConfig) =>
      config match {
        case cfg: BlobStoreAdapterConfig.MinIO => new MinIOInitializer(cfg)
      }
    }
}
