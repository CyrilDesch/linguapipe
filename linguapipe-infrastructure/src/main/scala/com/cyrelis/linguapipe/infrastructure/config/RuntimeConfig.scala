package com.cyrelis.linguapipe.infrastructure.config

import com.cyrelis.linguapipe.application.types.JobProcessingConfig
import zio.*

final case class RuntimeConfig(
  environment: String,
  api: ApiConfig,
  adapters: AdaptersConfig,
  migrations: MigrationConfig,
  fixtures: FixtureConfig,
  retry: RetryConfig,
  timeouts: TimeoutConfig,
  jobProcessing: JobProcessingConfig
)

final case class MigrationConfig(
  runOnStartup: Boolean,
  failOnError: Boolean
)

final case class FixtureConfig(
  loadOnStartup: Boolean
)

final case class RetryConfig(
  enabled: Boolean,
  maxRetries: Int,
  initialDelayMs: Long,
  maxDelayMs: Long,
  backoffFactor: Double
)

final case class TimeoutConfig(
  transcriptionMs: Long,
  embeddingMs: Long,
  databaseMs: Long,
  vectorStoreMs: Long,
  blobStoreMs: Long,
  documentParserMs: Long
)

final case class ApiConfig(host: String, port: Int)

final case class AdaptersConfig(
  driven: DrivenAdaptersConfig,
  driving: DrivingAdaptersConfig
)

final case class DrivenAdaptersConfig(
  database: DatabaseAdapterConfig,
  vectorStore: VectorStoreAdapterConfig,
  transcriber: TranscriberAdapterConfig,
  embedder: EmbedderAdapterConfig,
  blobStore: BlobStoreAdapterConfig,
  jobQueue: JobQueueAdapterConfig
)

final case class DrivingAdaptersConfig(
  api: ApiAdapterConfig
)

enum DatabaseAdapterConfig:
  case Postgres(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
  )

enum VectorStoreAdapterConfig:
  case Qdrant(url: String, apiKey: String, collection: String)

enum TranscriberAdapterConfig:
  case Whisper(modelPath: String, apiUrl: String)

enum EmbedderAdapterConfig:
  case HuggingFace(model: String, apiUrl: String)

enum BlobStoreAdapterConfig:
  case MinIO(endpoint: String, accessKey: String, secretKey: String, bucket: String)

enum JobQueueAdapterConfig:
  case Redis(
    host: String,
    port: Int,
    database: Int,
    password: Option[String],
    queueKey: String,
    deadLetterKey: String
  )

enum ApiAdapterConfig:
  case REST(host: String, port: Int)
  case GRPC(host: String, port: Int)

object RuntimeConfig {

  val layer: ZLayer[Any, Throwable, RuntimeConfig] =
    ZLayer.fromZIO(ConfigLoader.load)
}
