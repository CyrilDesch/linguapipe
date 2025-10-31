package com.cyrelis.linguapipe.infrastructure.config

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.datasource.DatasourcePort
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.job.JobQueuePort
import com.cyrelis.linguapipe.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.domain.ingestionjob.IngestionJobRepository
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import com.cyrelis.linguapipe.infrastructure.adapters.driven.blobstore.MinioAdapter
import com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres.{
  PostgresDatasource,
  PostgresJobRepository,
  PostgresTranscriptRepository
}
import com.cyrelis.linguapipe.infrastructure.adapters.driven.documentparser.PdfBoxParser
import com.cyrelis.linguapipe.infrastructure.adapters.driven.embedder.HuggingFaceAdapter
import com.cyrelis.linguapipe.infrastructure.adapters.driven.queue.RedisJobQueueAdapter
import com.cyrelis.linguapipe.infrastructure.adapters.driven.transcriber.WhisperAdapter
import com.cyrelis.linguapipe.infrastructure.adapters.driven.vectorstore.VectorStoreSink
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.IngestRestGateway
import zio.*

object AdapterFactory {

  def createDatasourceLayer(config: DatabaseAdapterConfig): ZLayer[Any, Throwable, DatasourcePort] =
    config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        PostgresDatasource.layer(cfg).map(ds => ZEnvironment(ds.get: DatasourcePort))
    }

  def createTranscriptRepositoryLayer(
    config: DatabaseAdapterConfig
  ): ZLayer[DatasourcePort, Nothing, TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]] =
    config match {
      case _: DatabaseAdapterConfig.Postgres =>
        ZLayer.fromFunction { (datasource: DatasourcePort) =>
          val quillDatasource = datasource.asInstanceOf[PostgresDatasource]
          new PostgresTranscriptRepository(quillDatasource.quillContext)
        }
    }

  def createJobRepositoryLayer(
    config: DatabaseAdapterConfig
  ): ZLayer[DatasourcePort, Nothing, IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]]] =
    config match {
      case _: DatabaseAdapterConfig.Postgres =>
        ZLayer.fromFunction { (datasource: DatasourcePort) =>
          val quillDatasource = datasource.asInstanceOf[PostgresDatasource]
          new PostgresJobRepository(quillDatasource.quillContext)
        }
    }

  def createVectorStoreAdapter(config: VectorStoreAdapterConfig): VectorStorePort =
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
        new HuggingFaceAdapter(cfg)
    }

  def createBlobStoreAdapter(config: BlobStoreAdapterConfig): BlobStorePort =
    config match {
      case cfg: BlobStoreAdapterConfig.MinIO =>
        new MinioAdapter(cfg.endpoint, cfg.accessKey, cfg.secretKey, cfg.bucket)
    }

  def createDocumentParserAdapter(): DocumentParserPort =
    new PdfBoxParser()

  def createJobQueueLayer(config: JobQueueAdapterConfig): ZLayer[Any, Throwable, JobQueuePort] =
    config match {
      case cfg: JobQueueAdapterConfig.Redis =>
        RedisJobQueueAdapter.layerFromConfig(cfg)
    }

  def createGateway(config: ApiAdapterConfig): Gateway =
    config match {
      case ApiAdapterConfig.REST(host, port) =>
        new IngestRestGateway(host, port)
      case ApiAdapterConfig.GRPC(host, port) =>
        throw new UnsupportedOperationException("gRPC gateway not yet implemented")
    }
}
