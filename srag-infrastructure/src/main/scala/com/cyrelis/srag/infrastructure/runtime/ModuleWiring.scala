package com.cyrelis.srag.infrastructure.runtime

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.ports.driven.datasource.DatasourcePort
import com.cyrelis.srag.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.srag.application.ports.driven.job.JobQueuePort
import com.cyrelis.srag.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.srag.application.ports.driven.reranker.RerankerPort
import com.cyrelis.srag.application.ports.driven.storage.{BlobStorePort, LexicalStorePort, VectorStorePort}
import com.cyrelis.srag.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.srag.application.ports.driving.{HealthCheckPort, IngestPort, QueryPort}
import com.cyrelis.srag.application.services.{DefaultHealthCheckService, DefaultIngestService, DefaultQueryService}
import com.cyrelis.srag.application.workers.DefaultIngestionJobWorker
import com.cyrelis.srag.domain.ingestionjob.IngestionJobRepository
import com.cyrelis.srag.domain.transcript.TranscriptRepository
import com.cyrelis.srag.infrastructure.adapters.driving.Gateway
import com.cyrelis.srag.infrastructure.config.{AdapterFactory, RuntimeConfig}
import com.cyrelis.srag.infrastructure.resilience.RetryWrappers
import zio.*

object ModuleWiring {

  val transcriberLayer: ZLayer[RuntimeConfig, Nothing, TranscriberPort] =
    ZLayer {
      for {
        config         <- ZIO.service[RuntimeConfig]
        baseTranscriber = AdapterFactory.createTranscriberAdapter(config.adapters.driven.transcriber)
        transcriber     = RetryWrappers.wrapTranscriber(baseTranscriber, config.retry, config.timeouts)
      } yield transcriber
    }

  val embedderLayer: ZLayer[RuntimeConfig, Nothing, EmbedderPort] =
    ZLayer {
      for {
        config      <- ZIO.service[RuntimeConfig]
        baseEmbedder = AdapterFactory.createEmbedderAdapter(config.adapters.driven.embedder)
        embedder     = RetryWrappers.wrapEmbedder(baseEmbedder, config.retry, config.timeouts)
      } yield embedder
    }

  val datasourceLayer: ZLayer[RuntimeConfig, Throwable, DatasourcePort] =
    ZLayer {
      for {
        config     <- ZIO.service[RuntimeConfig]
        datasource <- ZIO
                        .service[DatasourcePort]
                        .provide(
                          AdapterFactory.createDatasourceLayer(config.adapters.driven.database)
                        )
      } yield datasource
    }

  val transcriptRepositoryLayer
    : ZLayer[RuntimeConfig & DatasourcePort, Throwable, TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]] =
    ZLayer {
      for {
        config   <- ZIO.service[RuntimeConfig]
        _        <- ZIO.service[DatasourcePort]
        baseRepo <- ZIO
                      .service[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]]
                      .provideSome[DatasourcePort](
                        AdapterFactory.createTranscriptRepositoryLayer(config.adapters.driven.database)
                      )
        repo = RetryWrappers.wrapTranscriptRepository(baseRepo, config.retry, config.timeouts)
      } yield repo
    }

  val vectorSinkLayer: ZLayer[RuntimeConfig, Nothing, VectorStorePort] =
    ZLayer {
      for {
        config        <- ZIO.service[RuntimeConfig]
        baseVectorSink = AdapterFactory.createVectorStoreAdapter(config.adapters.driven.vectorStore)
        vectorSink     = RetryWrappers.wrapVectorSink(baseVectorSink, config.retry, config.timeouts)
      } yield vectorSink
    }

  val lexicalStoreLayer: ZLayer[RuntimeConfig, Nothing, LexicalStorePort] =
    ZLayer {
      for {
        config          <- ZIO.service[RuntimeConfig]
        baseLexicalStore = AdapterFactory.createLexicalStoreAdapter(config.adapters.driven.lexicalStore)
        lexicalStore     = RetryWrappers.wrapLexicalStore(baseLexicalStore, config.retry, config.timeouts)
      } yield lexicalStore
    }

  val rerankerLayer: ZLayer[RuntimeConfig, Nothing, RerankerPort] =
    ZLayer {
      for {
        config      <- ZIO.service[RuntimeConfig]
        baseReranker = AdapterFactory.createRerankerAdapter(config.adapters.driven.reranker)
        reranker     = RetryWrappers.wrapReranker(baseReranker, config.retry, config.timeouts)
      } yield reranker
    }

  val blobStoreLayer: ZLayer[RuntimeConfig, Nothing, BlobStorePort] =
    ZLayer {
      for {
        config       <- ZIO.service[RuntimeConfig]
        baseBlobStore = AdapterFactory.createBlobStoreAdapter(config.adapters.driven.blobStore)
        blobStore     = RetryWrappers.wrapBlobStore(baseBlobStore, config.retry, config.timeouts)
      } yield blobStore
    }

  val documentParserLayer: ZLayer[RuntimeConfig, Nothing, DocumentParserPort] =
    ZLayer {
      for {
        config            <- ZIO.service[RuntimeConfig]
        baseDocumentParser = AdapterFactory.createDocumentParserAdapter()
        documentParser     = RetryWrappers.wrapDocumentParser(baseDocumentParser, config.retry, config.timeouts)
      } yield documentParser
    }

  val jobRepositoryLayer
    : ZLayer[RuntimeConfig & DatasourcePort, Throwable, IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]]] =
    ZLayer {
      for {
        config  <- ZIO.service[RuntimeConfig]
        _       <- ZIO.service[DatasourcePort]
        jobRepo <- ZIO
                     .service[IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]]]
                     .provideSome[DatasourcePort](
                       AdapterFactory.createJobRepositoryLayer(config.adapters.driven.database)
                     )
        wrappedRepo = RetryWrappers.wrapJobRepository(jobRepo, config.retry, config.timeouts)
      } yield wrappedRepo
    }

  val jobQueueLayer: ZLayer[RuntimeConfig, Throwable, JobQueuePort] =
    ZLayer.fromZIO(ZIO.service[RuntimeConfig]).flatMap { env =>
      val config = env.get[RuntimeConfig]
      AdapterFactory.createJobQueueLayer(config.adapters.driven.jobQueue)
    }

  val gatewayLayer: ZLayer[RuntimeConfig, Nothing, Gateway] =
    ZLayer {
      for {
        config <- ZIO.service[RuntimeConfig]
        gateway = AdapterFactory.createGateway(config.adapters.driving.api)
      } yield gateway
    }

  val ingestServiceLayer: ZLayer[
    BlobStorePort & IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]] & JobQueuePort & RuntimeConfig,
    Nothing,
    IngestPort
  ] =
    ZLayer {
      for {
        blobStore     <- ZIO.service[BlobStorePort]
        jobRepository <- ZIO.service[IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]]]
        jobQueue      <- ZIO.service[JobQueuePort]
        config        <- ZIO.service[RuntimeConfig]
      } yield new DefaultIngestService(
        blobStore = blobStore,
        jobRepository = jobRepository,
        jobConfig = config.jobProcessing,
        jobQueue = jobQueue
      )
    }

  val jobWorkerLayer: ZLayer[
    IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]] & BlobStorePort & TranscriberPort & EmbedderPort &
      TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]] & VectorStorePort & LexicalStorePort & JobQueuePort &
      RuntimeConfig,
    Nothing,
    DefaultIngestionJobWorker
  ] =
    ZLayer {
      for {
        jobRepository        <- ZIO.service[IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]]]
        blobStore            <- ZIO.service[BlobStorePort]
        transcriber          <- ZIO.service[TranscriberPort]
        embedder             <- ZIO.service[EmbedderPort]
        transcriptRepository <- ZIO.service[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]]
        vectorSink           <- ZIO.service[VectorStorePort]
        lexicalStore         <- ZIO.service[LexicalStorePort]
        jobQueue             <- ZIO.service[JobQueuePort]
        config               <- ZIO.service[RuntimeConfig]
      } yield new DefaultIngestionJobWorker(
        jobRepository = jobRepository,
        blobStore = blobStore,
        transcriber = transcriber,
        embedder = embedder,
        transcriptRepository = transcriptRepository,
        vectorSink = vectorSink,
        lexicalStore = lexicalStore,
        jobConfig = config.jobProcessing,
        jobQueue = jobQueue
      )
    }

  val queryServiceLayer: ZLayer[
    EmbedderPort & VectorStorePort & LexicalStorePort & RerankerPort &
      TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]],
    Nothing,
    QueryPort
  ] =
    ZLayer {
      for {
        embedder             <- ZIO.service[EmbedderPort]
        vectorStore          <- ZIO.service[VectorStorePort]
        lexicalStore         <- ZIO.service[LexicalStorePort]
        reranker             <- ZIO.service[RerankerPort]
        transcriptRepository <- ZIO.service[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]]
      } yield new DefaultQueryService(
        embedder = embedder,
        vectorStore = vectorStore,
        lexicalStore = lexicalStore,
        reranker = reranker,
        transcriptRepository = transcriptRepository
      )
    }

  val healthCheckLayer: ZLayer[
    TranscriberPort & EmbedderPort & DatasourcePort & VectorStorePort & LexicalStorePort & RerankerPort &
      BlobStorePort & JobQueuePort,
    Nothing,
    HealthCheckPort
  ] =
    ZLayer {
      for {
        transcriber <- ZIO.service[TranscriberPort]
        embedder    <- ZIO.service[EmbedderPort]
        datasource  <- ZIO.service[DatasourcePort]
        vectorSink  <- ZIO.service[VectorStorePort]
        lexical     <- ZIO.service[LexicalStorePort]
        reranker    <- ZIO.service[RerankerPort]
        blobStore   <- ZIO.service[BlobStorePort]
        jobQueue    <- ZIO.service[JobQueuePort]
      } yield new DefaultHealthCheckService(
        transcriber = transcriber,
        embedder = embedder,
        datasource = datasource,
        vectorSink = vectorSink,
        lexicalStore = lexical,
        reranker = reranker,
        blobStore = blobStore,
        jobQueue = jobQueue
      )
    }
}
