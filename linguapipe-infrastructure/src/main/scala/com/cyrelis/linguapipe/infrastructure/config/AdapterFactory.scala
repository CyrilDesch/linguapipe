package com.cyrelis.linguapipe.infrastructure.config

import com.cyrelis.linguapipe.application.ports.driven.*
import com.cyrelis.linguapipe.infrastructure.adapters.driven.blobstore.MinioAdapter
import com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres.{PostgresTranscriptSink, QuillContext}
import com.cyrelis.linguapipe.infrastructure.adapters.driven.documentparser.PdfBoxParser
import com.cyrelis.linguapipe.infrastructure.adapters.driven.embedder.EmbeddingService
import com.cyrelis.linguapipe.infrastructure.adapters.driven.transcriber.WhisperAdapter
import com.cyrelis.linguapipe.infrastructure.adapters.driven.vectorstore.VectorStoreSink
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.IngestRestGateway
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object AdapterFactory {

  def createDatabaseAdapterLayer(config: DatabaseAdapterConfig): ZLayer[Any, Throwable, DbSinkPort] =
    config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        QuillContext.createLayer(cfg) >>>
          ZLayer.fromFunction((quill: Quill.Postgres[SnakeCase]) => new PostgresTranscriptSink(cfg, quill))
    }

  def createDatabaseAdapter(config: DatabaseAdapterConfig): DbSinkPort =
    throw new UnsupportedOperationException(
      "Database adapter now requires ZLayer. Use createDatabaseAdapterLayer instead."
    )

  def createVectorStoreAdapter(config: VectorStoreAdapterConfig): VectorSinkPort =
    config match {
      case cfg: VectorStoreAdapterConfig.Qdrant =>
        new VectorStoreSink(s"qdrant://${cfg.url}/${cfg.collection}")
    }

  def createTranscriberAdapter(config: TranscriberAdapterConfig): TranscriberPort =
    config match {
      case cfg: TranscriberAdapterConfig.Whisper =>
        new WhisperAdapter(cfg)
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

  def createDocumentParserAdapter(): DocumentParserPort =
    new PdfBoxParser()

  def createGateway(config: ApiAdapterConfig): Gateway =
    config match {
      case ApiAdapterConfig.REST(host, port) =>
        new IngestRestGateway(host, port)
      case ApiAdapterConfig.GRPC(host, port) =>
        throw new UnsupportedOperationException("gRPC gateway not yet implemented")
    }
}
