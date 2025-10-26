package linguapipe.infrastructure

import zio.*

import linguapipe.infrastructure.config.RuntimeConfig
import linguapipe.infrastructure.migration.MigrationRunner
import linguapipe.infrastructure.runtime.{LinguaPipeRuntime, ModuleWiring}

/**
 * Main entry point for the LinguaPipe application. Configuration is loaded from
 * application.conf and adapters are automatically instantiated based on this
 * configuration.
 *
 * Startup sequence:
 *   1. Load configuration
 *   2. Run migrations and initializations
 *   3. Wire adapters
 *   4. Start application
 */
object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    (for {
      _      <- ZIO.logInfo("Starting LinguaPipe application...")
      config <- ZIO.service[RuntimeConfig]
      _      <- ZIO.logInfo(s"Environment: ${config.environment}")
      _      <- ZIO.logInfo(s"Database: ${config.adapters.driven.database.getClass.getSimpleName}")
      _      <- ZIO.logInfo(s"Vector Store: ${config.adapters.driven.vectorStore.getClass.getSimpleName}")
      _      <- ZIO.logInfo(s"Transcriber: ${config.adapters.driven.transcriber.getClass.getSimpleName}")

      // Run migrations and initializations before starting the app (if configured)
      _ <- if (config.migrations.runOnStartup) {
             MigrationRunner.runAll().catchAll { error =>
               if (config.migrations.failOnError) {
                 ZIO.logError(s"❌ Migration failed and fail-on-error is enabled: ${error.getMessage}") *>
                   ZIO.fail(error)
               } else {
                 ZIO.logWarning(s"⚠️  Migration failed but continuing (fail-on-error is disabled): ${error.getMessage}")
               }
             }
           } else {
             ZIO.logInfo("ℹ️  Skipping migrations (run-on-startup is disabled)")
           }

      _               <- ZIO.service[linguapipe.application.ports.driving.IngestPort]
      healthCheckPort <- ZIO.service[linguapipe.application.ports.driving.HealthCheckPort]
      _               <- LinguaPipeRuntime.make(healthCheckPort)
      _               <- ZIO.logInfo("✅ LinguaPipe started successfully")
      _               <- ZIO.never
    } yield ())
      .provide(
        RuntimeConfig.layer.tapError(err => ZIO.logError(s"Failed to load configuration: ${err.getMessage}")).orDie,
        ModuleWiring.pipelineLayer,
        ModuleWiring.healthCheckLayer
      )
}
