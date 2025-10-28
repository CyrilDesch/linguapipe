package com.cyrelis.linguapipe.infrastructure.runtime

import zio.*

import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.application.usecase.{DefaultHealthCheckUseCase, DefaultIngestPipeline}
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.config.{AdapterFactory, RuntimeConfig}

object ModuleWiring {

  val pipelineLayer: ZLayer[RuntimeConfig, Nothing, IngestPort] =
    ZLayer {
      for {
        config        <- ZIO.service[RuntimeConfig]
        adaptersConfig = config.adapters.driven

        transcriber    = AdapterFactory.createTranscriberAdapter(adaptersConfig.transcriber)
        embedder       = AdapterFactory.createEmbedderAdapter(adaptersConfig.embedder)
        dbSink         = AdapterFactory.createDatabaseAdapter(adaptersConfig.database)
        vectorSink     = AdapterFactory.createVectorStoreAdapter(adaptersConfig.vectorStore)
        blobStore      = AdapterFactory.createBlobStoreAdapter(adaptersConfig.blobStore)
        documentParser = AdapterFactory.createDocumentParserAdapter()

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

        transcriber = AdapterFactory.createTranscriberAdapter(adaptersConfig.transcriber)
        embedder    = AdapterFactory.createEmbedderAdapter(adaptersConfig.embedder)
        dbSink      = AdapterFactory.createDatabaseAdapter(adaptersConfig.database)
        vectorSink  = AdapterFactory.createVectorStoreAdapter(adaptersConfig.vectorStore)
        blobStore   = AdapterFactory.createBlobStoreAdapter(adaptersConfig.blobStore)

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
