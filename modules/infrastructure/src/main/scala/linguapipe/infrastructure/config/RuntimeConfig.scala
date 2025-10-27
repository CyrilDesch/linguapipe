package linguapipe.infrastructure.config

import zio.*

final case class RuntimeConfig(
  environment: String,
  api: ApiConfig,
  adapters: AdaptersConfig,
  migrations: MigrationConfig
)

final case class MigrationConfig(
  runOnStartup: Boolean,
  failOnError: Boolean
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
  blobStore: BlobStoreAdapterConfig
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

enum ApiAdapterConfig:
  case REST(host: String, port: Int)
  case GRPC(host: String, port: Int)

object RuntimeConfig {

  val layer: ZLayer[Any, Throwable, RuntimeConfig] =
    ZLayer.fromZIO(ConfigLoader.load)
}
