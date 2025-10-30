package com.cyrelis.linguapipe.infrastructure.runtime

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.datasource.DatasourcePort
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.job.JobQueuePort
import com.cyrelis.linguapipe.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.application.services.{DefaultHealthCheckService, DefaultIngestService}
import com.cyrelis.linguapipe.application.workers.DefaultIngestionJobWorker
import com.cyrelis.linguapipe.domain.ingestionjob.IngestionJobRepository
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.config.{AdapterFactory, RuntimeConfig}
import com.cyrelis.linguapipe.infrastructure.resilience.RetryWrappers
import zio.*

object ModuleWiring {

  // Individual adapter layers
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

  // Use case layers
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
      TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]] & VectorStorePort & JobQueuePort & RuntimeConfig,
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
        jobQueue             <- ZIO.service[JobQueuePort]
        config               <- ZIO.service[RuntimeConfig]
      } yield new DefaultIngestionJobWorker(
        jobRepository = jobRepository,
        blobStore = blobStore,
        transcriber = transcriber,
        embedder = embedder,
        transcriptRepository = transcriptRepository,
        vectorSink = vectorSink,
        jobConfig = config.jobProcessing,
        jobQueue = jobQueue
      )
    }

  val healthCheckLayer: ZLayer[
    TranscriberPort & EmbedderPort & DatasourcePort & VectorStorePort & BlobStorePort & JobQueuePort,
    Nothing,
    HealthCheckPort
  ] =
    ZLayer {
      for {
        transcriber <- ZIO.service[TranscriberPort]
        embedder    <- ZIO.service[EmbedderPort]
        datasource  <- ZIO.service[DatasourcePort]
        vectorSink  <- ZIO.service[VectorStorePort]
        blobStore   <- ZIO.service[BlobStorePort]
        jobQueue    <- ZIO.service[JobQueuePort]
      } yield new DefaultHealthCheckService(
        transcriber = transcriber,
        embedder = embedder,
        datasource = datasource,
        vectorSink = vectorSink,
        blobStore = blobStore,
        jobQueue = jobQueue
      )
    }
}
