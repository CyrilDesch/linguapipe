package linguapipe.infrastructure.runtime

import zio.*

import linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import linguapipe.application.usecase.{DefaultHealthCheckUseCase, DefaultIngestPipeline}
import linguapipe.infrastructure.config.{AdapterFactory, RuntimeConfig}

/**
 * Wiring using declarative configuration to inject adapters into the
 * application via ports.
 */
object ModuleWiring {

  /**
   * Layer creating the ingestion pipeline with dynamically configured adapters
   */
  val pipelineLayer: ZLayer[RuntimeConfig, Nothing, IngestPort] =
    ZLayer {
      for {
        config        <- ZIO.service[RuntimeConfig]
        adaptersConfig = config.adapters.driven

        // Create adapters based on declarative configuration
        transcriber = AdapterFactory.createTranscriberAdapter(adaptersConfig.transcriber)
        embedder    = AdapterFactory.createEmbedderAdapter(adaptersConfig.embedder)
        dbSink      = AdapterFactory.createDatabaseAdapter(adaptersConfig.database)
        vectorSink  = AdapterFactory.createVectorStoreAdapter(adaptersConfig.vectorStore)
        blobStore   = AdapterFactory.createBlobStoreAdapter(adaptersConfig.blobStore)

      } yield new DefaultIngestPipeline(
        transcriber = transcriber,
        embedder = embedder,
        dbSink = dbSink,
        vectorSink = vectorSink,
        blobStore = blobStore
      )
    }

  /**
   * Layer creating the health check use case with dynamically configured
   * adapters
   */
  val healthCheckLayer: ZLayer[RuntimeConfig, Nothing, HealthCheckPort] =
    ZLayer {
      for {
        config        <- ZIO.service[RuntimeConfig]
        adaptersConfig = config.adapters.driven

        // Create adapters based on declarative configuration
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
}
