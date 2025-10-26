package linguapipe.infrastructure.config

import linguapipe.application.ports.driven.*
import linguapipe.infrastructure.adapters.driven.blobstore.BlobStoreAdapter
import linguapipe.infrastructure.adapters.driven.database.postgres.PostgresTranscriptSink
import linguapipe.infrastructure.adapters.driven.embedder.EmbeddingService
import linguapipe.infrastructure.adapters.driven.transcriptor.TranscriptionService
import linguapipe.infrastructure.adapters.driven.vectorstore.VectorStoreSink

/** Factory for creating adapter instances based on declarative configuration */
object AdapterFactory {

  /** Creates database adapter based on configuration */
  def createDatabaseAdapter(config: DatabaseAdapterConfig): DbSinkPort =
    config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        new PostgresTranscriptSink(cfg)
    }

  /** Creates vector store adapter based on configuration */
  def createVectorStoreAdapter(config: VectorStoreAdapterConfig): VectorSinkPort =
    config match {
      case cfg: VectorStoreAdapterConfig.Qdrant =>
        new VectorStoreSink(s"qdrant://${cfg.url}/${cfg.collection}")
    }

  /** Creates transcription adapter based on configuration */
  def createTranscriberAdapter(config: TranscriberAdapterConfig): TranscriberPort =
    config match {
      case cfg: TranscriberAdapterConfig.Whisper =>
        new TranscriptionService(s"whisper:${cfg.modelPath}")
    }

  /** Creates embedding adapter based on configuration */
  def createEmbedderAdapter(config: EmbedderAdapterConfig): EmbedderPort =
    config match {
      case cfg: EmbedderAdapterConfig.HuggingFace =>
        new EmbeddingService(s"huggingface:${cfg.model}")
    }

  /** Creates blob store adapter based on configuration */
  def createBlobStoreAdapter(config: BlobStoreAdapterConfig): BlobStorePort =
    config match {
      case cfg: BlobStoreAdapterConfig.MinIO =>
        new BlobStoreAdapter(s"minio://${cfg.endpoint}/${cfg.bucket}")
    }
}
