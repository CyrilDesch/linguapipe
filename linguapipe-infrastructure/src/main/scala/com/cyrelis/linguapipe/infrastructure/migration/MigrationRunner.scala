package com.cyrelis.linguapipe.infrastructure.migration

import com.cyrelis.linguapipe.infrastructure.config.{BlobStoreAdapterConfig, RuntimeConfig, VectorStoreAdapterConfig}
import zio.*

/**
 * Orchestrates all migration and initialization tasks at application startup.
 * This runs BEFORE the application starts accepting requests.
 */
object MigrationRunner {

  def runAll(): ZIO[RuntimeConfig, Throwable, Unit] =
    for {
      config <- ZIO.service[RuntimeConfig]
      _      <- ZIO.logInfo("Starting migration and initialization...")
      _      <- runDatabaseMigrations()
      _      <- if (config.fixtures.loadOnStartup) loadDatabaseFixtures() else ZIO.unit
      _      <- initializeVectorStore()
      _      <- initializeBlobStore()
      _      <- ZIO.logInfo("All migrations and initializations completed successfully")
    } yield ()

  private def runDatabaseMigrations(): ZIO[RuntimeConfig, Throwable, Unit] =
    for {
      config          <- ZIO.service[RuntimeConfig]
      dbConfig         = config.adapters.driven.database
      migrationService = new FlywayMigrationService(dbConfig)
      _               <- migrationService.runMigrations()
    } yield ()

  private def loadDatabaseFixtures(): ZIO[RuntimeConfig, Throwable, Unit] =
    for {
      config        <- ZIO.service[RuntimeConfig]
      dbConfig       = config.adapters.driven.database
      fixtureService = new com.cyrelis.linguapipe.infrastructure.fixtures.DatabaseFixtureService(dbConfig)
      hasData       <- fixtureService.hasData()
      _             <- if (hasData) {
             ZIO.logInfo("Database already contains data, skipping fixtures")
           } else {
             ZIO.logInfo("Database is empty, loading fixtures") *>
               fixtureService.loadFixtures()
           }
    } yield ()

  private def initializeVectorStore(): ZIO[RuntimeConfig, Throwable, Unit] =
    for {
      config      <- ZIO.service[RuntimeConfig]
      vectorConfig = config.adapters.driven.vectorStore
      initializer <- ZIO.succeed(createVectorStoreInitializer(vectorConfig))
      _           <- initializer.initialize()
    } yield ()

  private def initializeBlobStore(): ZIO[RuntimeConfig, Throwable, Unit] =
    for {
      config      <- ZIO.service[RuntimeConfig]
      blobConfig   = config.adapters.driven.blobStore
      initializer <- ZIO.succeed(createBlobStoreInitializer(blobConfig))
      _           <- initializer.initialize()
    } yield ()

  private def createVectorStoreInitializer(config: VectorStoreAdapterConfig): VectorStoreInitializer =
    config match {
      case cfg: VectorStoreAdapterConfig.Qdrant => new VectorStoreInitializer.QdrantInitializer(cfg)
    }

  private def createBlobStoreInitializer(config: BlobStoreAdapterConfig): BlobStoreInitializer =
    config match {
      case cfg: BlobStoreAdapterConfig.MinIO => new BlobStoreInitializer.MinIOInitializer(cfg)
    }
}
