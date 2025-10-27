package linguapipe.infrastructure.config

import linguapipe.application.ports.driven.*
import linguapipe.infrastructure.adapters.driven.blobstore.MinioAdapter
import linguapipe.infrastructure.adapters.driven.database.postgres.PostgresTranscriptSink
import linguapipe.infrastructure.adapters.driven.embedder.EmbeddingService
import linguapipe.infrastructure.adapters.driven.transcriptor.TranscriptionService
import linguapipe.infrastructure.adapters.driven.vectorstore.VectorStoreSink
import linguapipe.infrastructure.adapters.driving.Gateway
import linguapipe.infrastructure.adapters.driving.gateway.rest.IngestRestGateway

object AdapterFactory {

  def createDatabaseAdapter(config: DatabaseAdapterConfig): DbSinkPort =
    config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        new PostgresTranscriptSink(cfg)
    }

  def createVectorStoreAdapter(config: VectorStoreAdapterConfig): VectorSinkPort =
    config match {
      case cfg: VectorStoreAdapterConfig.Qdrant =>
        new VectorStoreSink(s"qdrant://${cfg.url}/${cfg.collection}")
    }

  def createTranscriberAdapter(config: TranscriberAdapterConfig): TranscriberPort =
    config match {
      case cfg: TranscriberAdapterConfig.Whisper =>
        new TranscriptionService(s"whisper:${cfg.modelPath}")
    }

  def createEmbedderAdapter(config: EmbedderAdapterConfig): EmbedderPort =
    config match {
      case cfg: EmbedderAdapterConfig.HuggingFace =>
        new EmbeddingService(s"huggingface:${cfg.model}")
    }

  def createBlobStoreAdapter(config: BlobStoreAdapterConfig): BlobStorePort =
    config match {
      case cfg: BlobStoreAdapterConfig.MinIO =>
        new MinioAdapter(cfg.endpoint, cfg.accessKey, cfg.secretKey, cfg.bucket)
    }

  def createGateway(config: ApiAdapterConfig): Gateway =
    config match {
      case ApiAdapterConfig.REST(host, port) =>
        new IngestRestGateway(host, port)
      case ApiAdapterConfig.GRPC(host, port) =>
        throw new UnsupportedOperationException("gRPC gateway not yet implemented")
    }
}
