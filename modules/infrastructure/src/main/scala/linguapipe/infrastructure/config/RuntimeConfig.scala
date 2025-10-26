package linguapipe.infrastructure.config

import zio.*

/** Runtime configuration facade. */
final case class RuntimeConfig(
  environment: String,
  grpc: GrpcConfig,
  adapters: AdaptersConfig,
  migrations: MigrationConfig
)

final case class MigrationConfig(
  runOnStartup: Boolean,
  failOnError: Boolean
)

final case class GrpcConfig(host: String, port: Int)

/** Declarative configuration for driven and driving adapters */
final case class AdaptersConfig(
  driven: DrivenAdaptersConfig,
  driving: DrivingAdaptersConfig
)

final case class DrivenAdaptersConfig(
  database: DatabaseAdapterConfig,
  vectorStore: VectorStoreAdapterConfig,
  transcriber: TranscriberAdapterConfig,
  embedder: EmbedderAdapterConfig,
  blobStore: BlobStoreAdapterConfig
)

final case class DrivingAdaptersConfig(
  api: ApiAdapterConfig
)

/** Database adapter configuration */
enum DatabaseAdapterConfig:
  case Postgres(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
  )

/** Vector store adapter configuration */
enum VectorStoreAdapterConfig:
  case Qdrant(url: String, apiKey: String, collection: String)

/** Transcription adapter configuration */
enum TranscriberAdapterConfig:
  case Whisper(modelPath: String, apiUrl: String)

/** Embedding adapter configuration */
enum EmbedderAdapterConfig:
  case HuggingFace(model: String, apiUrl: String)

/** Blob store adapter configuration */
enum BlobStoreAdapterConfig:
  case MinIO(endpoint: String, accessKey: String, secretKey: String, bucket: String)

/** API adapter configuration (driving) */
enum ApiAdapterConfig:
  case GRPC(host: String, port: Int)

object RuntimeConfig {

  /** Loads configuration from application.conf */
  val layer: ZLayer[Any, Throwable, RuntimeConfig] =
    ZLayer.fromZIO(ConfigLoader.load)
}
