package com.cyrelis.linguapipe.infrastructure.runtime

import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.application.usecase.{DefaultHealthCheckUseCase, DefaultIngestPipeline}
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.config.{AdapterFactory, RuntimeConfig}
import com.cyrelis.linguapipe.infrastructure.resilience.RetryWrappers
import zio.*

object ModuleWiring {

  val pipelineLayer: ZLayer[RuntimeConfig, Nothing, IngestPort] =
    ZLayer {
      for {
        config        <- ZIO.service[RuntimeConfig]
        adaptersConfig = config.adapters.driven

        baseTranscriber    = AdapterFactory.createTranscriberAdapter(adaptersConfig.transcriber)
        baseEmbedder       = AdapterFactory.createEmbedderAdapter(adaptersConfig.embedder)
        baseDbSink         = AdapterFactory.createDatabaseAdapter(adaptersConfig.database)
        baseVectorSink     = AdapterFactory.createVectorStoreAdapter(adaptersConfig.vectorStore)
        baseBlobStore      = AdapterFactory.createBlobStoreAdapter(adaptersConfig.blobStore)
        baseDocumentParser = AdapterFactory.createDocumentParserAdapter()

        transcriber    = RetryWrappers.wrapTranscriber(baseTranscriber, config.retry, config.timeouts)
        embedder       = RetryWrappers.wrapEmbedder(baseEmbedder, config.retry, config.timeouts)
        dbSink         = RetryWrappers.wrapDbSink(baseDbSink, config.retry, config.timeouts)
        vectorSink     = RetryWrappers.wrapVectorSink(baseVectorSink, config.retry, config.timeouts)
        blobStore      = RetryWrappers.wrapBlobStore(baseBlobStore, config.retry, config.timeouts)
        documentParser = RetryWrappers.wrapDocumentParser(baseDocumentParser, config.retry, config.timeouts)

      } yield new DefaultIngestPipeline(
        transcriber = transcriber,
        embedder = embedder,
        dbSink = dbSink,
        vectorSink = vectorSink,
        blobStore = blobStore,
        documentParser = documentParser
      )
    }

  val healthCheckLayer: ZLayer[RuntimeConfig, Nothing, HealthCheckPort] =
    ZLayer {
      for {
        config        <- ZIO.service[RuntimeConfig]
        adaptersConfig = config.adapters.driven

        baseTranscriber = AdapterFactory.createTranscriberAdapter(adaptersConfig.transcriber)
        baseEmbedder    = AdapterFactory.createEmbedderAdapter(adaptersConfig.embedder)
        baseDbSink      = AdapterFactory.createDatabaseAdapter(adaptersConfig.database)
        baseVectorSink  = AdapterFactory.createVectorStoreAdapter(adaptersConfig.vectorStore)
        baseBlobStore   = AdapterFactory.createBlobStoreAdapter(adaptersConfig.blobStore)

        transcriber = RetryWrappers.wrapTranscriber(baseTranscriber, config.retry, config.timeouts)
        embedder    = RetryWrappers.wrapEmbedder(baseEmbedder, config.retry, config.timeouts)
        dbSink      = RetryWrappers.wrapDbSink(baseDbSink, config.retry, config.timeouts)
        vectorSink  = RetryWrappers.wrapVectorSink(baseVectorSink, config.retry, config.timeouts)
        blobStore   = RetryWrappers.wrapBlobStore(baseBlobStore, config.retry, config.timeouts)

      } yield new DefaultHealthCheckUseCase(
        transcriber = transcriber,
        embedder = embedder,
        dbSink = dbSink,
        vectorSink = vectorSink,
        blobStore = blobStore
      )
    }

  val gatewayLayer: ZLayer[RuntimeConfig, Nothing, Gateway] =
    ZLayer {
      for {
        config <- ZIO.service[RuntimeConfig]
        gateway = AdapterFactory.createGateway(config.adapters.driving.api)
      } yield gateway
    }
}
