package com.cyrelis.linguapipe.infrastructure

import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.config.RuntimeConfig
import com.cyrelis.linguapipe.infrastructure.migration.MigrationRunner
import com.cyrelis.linguapipe.infrastructure.runtime.ModuleWiring
import zio.*

object Main extends ZIOAppDefault {

  type AppDependencies = RuntimeConfig & IngestPort & HealthCheckPort & Gateway

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> zio.logging.backend.SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    startup.provide(
      RuntimeConfig.layer
        .tapError(err => ZIO.logError(s"Failed to load configuration: ${err.getMessage}"))
        .orDie,
      ModuleWiring.pipelineLayer,
      ModuleWiring.healthCheckLayer,
      ModuleWiring.gatewayLayer
    )

  private def startup: ZIO[AppDependencies, Nothing, Unit] =
    (for {
      _ <- ZIO.logInfo("Starting LinguaPipe application...")
      _ <- runMigrations
      _ <- runHealthChecks
      _ <- startGateway
      _ <- ZIO.never
    } yield ()).orDie

  private def runMigrations: ZIO[RuntimeConfig, Throwable, Unit] =
    for {
      config <- ZIO.service[RuntimeConfig]
      _      <- if (config.migrations.runOnStartup) {
             MigrationRunner.runAll().catchAll { error =>
               if (config.migrations.failOnError) {
                 ZIO.logError(s"Migration failed and fail-on-error is enabled: ${error.getMessage}") *>
                   ZIO.fail(error)
               } else {
                 ZIO.logWarning(s"Migration failed but continuing (fail-on-error is disabled): ${error.getMessage}")
               }
             }
           } else {
             ZIO.logInfo("Skipping migrations (run-on-startup is disabled)")
           }
    } yield ()

  private def runHealthChecks: ZIO[HealthCheckPort, Throwable, Unit] =
    for {
      healthCheckPort <- ZIO.service[HealthCheckPort]
      _               <- ZIO.logInfo("Running health checks...")
      healthResults   <- healthCheckPort.checkAllServices().orElse(ZIO.succeed(List.empty))
      _               <- logHealthResults(healthResults)
      _               <- ZIO.logInfo("All systems operational")
    } yield ()

  private def logHealthResults(results: List[HealthStatus]): ZIO[Any, Throwable, Unit] =
    ZIO
      .foreach(results) { status =>
        status match {
          case HealthStatus.Healthy(serviceName, _, _) =>
            ZIO.logInfo(s"✓ $serviceName")
          case HealthStatus.Unhealthy(serviceName, _, error, _) =>
            ZIO.logWarning(s"✗ $serviceName: $error")
          case HealthStatus.Timeout(serviceName, _, timeoutMs) =>
            ZIO.logWarning(s"✗ $serviceName: timeout after ${timeoutMs}ms")
        }
      }
      .unit

  private def startGateway: ZIO[Gateway & IngestPort & HealthCheckPort & RuntimeConfig, Throwable, Unit] =
    for {
      gateway <- ZIO.service[Gateway]
      _       <- gateway.start
    } yield ()
}
