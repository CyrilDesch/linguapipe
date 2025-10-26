package linguapipe.infrastructure.config

import com.typesafe.config.{Config, ConfigFactory}
import zio.*

/**
 * Charge la configuration depuis application.conf et crée les instances de
 * configuration typées.
 */
object ConfigLoader {

  /**
   * Charge la configuration complète depuis application.conf
   */
  def load: Task[RuntimeConfig] = ZIO.attempt {
    val config = ConfigFactory.load().getConfig("linguapipe")

    RuntimeConfig(
      environment = config.getString("runtime.environment"),
      grpc = loadGrpcConfig(config),
      adapters = loadAdaptersConfig(config),
      migrations = loadMigrationConfig(config)
    )
  }

  private def loadMigrationConfig(config: Config): MigrationConfig =
    MigrationConfig(
      runOnStartup = if (config.hasPath("migrations.run-on-startup")) {
        config.getBoolean("migrations.run-on-startup")
      } else true,
      failOnError = if (config.hasPath("migrations.fail-on-error")) {
        config.getBoolean("migrations.fail-on-error")
      } else true
    )

  private def loadGrpcConfig(config: Config): GrpcConfig =
    GrpcConfig(
      host = config.getString("grpc.host"),
      port = config.getInt("grpc.port")
    )

  private def loadAdaptersConfig(config: Config): AdaptersConfig =
    AdaptersConfig(
      driven = loadDrivenAdaptersConfig(config.getConfig("adapters.driven")),
      driving = loadDrivingAdaptersConfig(config.getConfig("adapters.driving"))
    )

  private def loadDrivenAdaptersConfig(config: Config): DrivenAdaptersConfig =
    DrivenAdaptersConfig(
      database = loadDatabaseConfig(config.getConfig("database")),
      vectorStore = loadVectorStoreConfig(config.getConfig("vector-store")),
      transcriber = loadTranscriberConfig(config.getConfig("transcriber")),
      embedder = loadEmbedderConfig(config.getConfig("embedder")),
      blobStore = loadBlobStoreConfig(config.getConfig("blob-store"))
    )

  private def loadDatabaseConfig(config: Config): DatabaseAdapterConfig = {
    val dbType = config.getString("type")
    dbType match {
      case "postgres" =>
        val pgConfig = config.getConfig("postgres")
        DatabaseAdapterConfig.Postgres(
          host = pgConfig.getString("host"),
          port = pgConfig.getInt("port"),
          database = pgConfig.getString("database"),
          user = pgConfig.getString("user"),
          password = pgConfig.getString("password")
        )

      case other =>
        throw new IllegalArgumentException(s"Unknown database type: $other")
    }
  }

  private def loadVectorStoreConfig(config: Config): VectorStoreAdapterConfig = {
    val vsType = config.getString("type")
    vsType match {
      case "qdrant" =>
        val qdConfig = config.getConfig("qdrant")
        VectorStoreAdapterConfig.Qdrant(
          url = qdConfig.getString("url"),
          apiKey =
            if (qdConfig.hasPath("api-key") && !qdConfig.getString("api-key").isEmpty)
              qdConfig.getString("api-key")
            else "",
          collection = qdConfig.getString("collection")
        )

      case other =>
        throw new IllegalArgumentException(s"Unknown vector-store type: $other")
    }
  }

  private def loadTranscriberConfig(config: Config): TranscriberAdapterConfig = {
    val tType = config.getString("type")
    tType match {
      case "whisper" =>
        val whConfig = config.getConfig("whisper")
        TranscriberAdapterConfig.Whisper(
          modelPath = whConfig.getString("model-path"),
          apiUrl = whConfig.getString("api-url")
        )

      case other =>
        throw new IllegalArgumentException(s"Unknown transcriber type: $other")
    }
  }

  private def loadEmbedderConfig(config: Config): EmbedderAdapterConfig = {
    val eType = config.getString("type")
    eType match {
      case "huggingface" =>
        val hfConfig = config.getConfig("huggingface")
        EmbedderAdapterConfig.HuggingFace(
          model = hfConfig.getString("model"),
          apiUrl = hfConfig.getString("api-url")
        )

      case other =>
        throw new IllegalArgumentException(s"Unknown embedder type: $other")
    }
  }

  private def loadBlobStoreConfig(config: Config): BlobStoreAdapterConfig = {
    val bType = config.getString("type")
    bType match {
      case "minio" =>
        val minioConfig = config.getConfig("minio")
        BlobStoreAdapterConfig.MinIO(
          endpoint = minioConfig.getString("endpoint"),
          accessKey = minioConfig.getString("access-key"),
          secretKey = minioConfig.getString("secret-key"),
          bucket = minioConfig.getString("bucket")
        )

      case other =>
        throw new IllegalArgumentException(s"Unknown blob-store type: $other")
    }
  }

  private def loadDrivingAdaptersConfig(config: Config): DrivingAdaptersConfig =
    DrivingAdaptersConfig(
      api = loadApiConfig(config.getConfig("api"))
    )

  private def loadApiConfig(config: Config): ApiAdapterConfig = {
    val apiType = config.getString("type")
    apiType match {
      case "grpc" =>
        val grpcConfig = config.getConfig("grpc")
        ApiAdapterConfig.GRPC(
          host = grpcConfig.getString("host"),
          port = grpcConfig.getInt("port")
        )

      case other =>
        throw new IllegalArgumentException(s"Unknown api type: $other")
    }
  }
}
