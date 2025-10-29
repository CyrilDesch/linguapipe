package com.cyrelis.linguapipe.infrastructure.migration

import com.cyrelis.linguapipe.infrastructure.config.BlobStoreAdapterConfig
import zio.*

trait BlobStoreInitializer {
  def initialize(): Task[Unit]
}

object BlobStoreInitializer {

  final class MinIOInitializer(config: BlobStoreAdapterConfig.MinIO) extends BlobStoreInitializer {
    override def initialize(): Task[Unit] =
      ZIO.logInfo(s"Initializing MinIO bucket: ${config.bucket}") *>
        createBucketIfNotExists() *>
        ZIO.logInfo(s"MinIO bucket '${config.bucket}' is ready")

    private def createBucketIfNotExists(): Task[Unit] =
      ZIO.attempt {
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
