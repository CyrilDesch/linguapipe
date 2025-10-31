package com.cyrelis.linguapipe.infrastructure

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.datasource.DatasourcePort
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.job.JobQueuePort
import com.cyrelis.linguapipe.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.linguapipe.application.ports.driven.reranker.RerankerPort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, LexicalStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.application.workers.DefaultIngestionJobWorker
import com.cyrelis.linguapipe.domain.ingestionjob.IngestionJobRepository
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.config.RuntimeConfig
import com.cyrelis.linguapipe.infrastructure.migration.MigrationRunner
import com.cyrelis.linguapipe.infrastructure.runtime.ModuleWiring
import zio.*

object Main extends ZIOAppDefault {

  type AllPorts = TranscriberPort & EmbedderPort & TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]] &
    VectorStorePort & LexicalStorePort & RerankerPort & BlobStorePort & DocumentParserPort &
    IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]] & JobQueuePort & DatasourcePort
  type AppDependencies = RuntimeConfig & IngestPort & HealthCheckPort & Gateway & AllPorts & DefaultIngestionJobWorker

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> zio.logging.backend.SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    startup.provide(
      RuntimeConfig.layer
        .tapError(err => ZIO.logError(s"Failed to load configuration: ${err.getMessage}"))
        .orDie,
      // Adapter layers
      ModuleWiring.transcriberLayer,
      ModuleWiring.embedderLayer,
      ModuleWiring.datasourceLayer,
      ModuleWiring.transcriptRepositoryLayer,
      ModuleWiring.vectorSinkLayer,
      ModuleWiring.lexicalStoreLayer,
      ModuleWiring.rerankerLayer,
      ModuleWiring.blobStoreLayer,
      ModuleWiring.documentParserLayer,
      ModuleWiring.jobRepositoryLayer,
      ModuleWiring.jobQueueLayer,
      // Use case layers
      ModuleWiring.ingestServiceLayer,
      ModuleWiring.jobWorkerLayer,
      ModuleWiring.healthCheckLayer,
      // Gateway layer
      ModuleWiring.gatewayLayer
    )

  private def startup: ZIO[AppDependencies, Nothing, Unit] =
    (for {
      _           <- ZIO.logInfo("Starting LinguaPipe application...")
      _           <- runMigrations
      _           <- ensureAllHealthy
      worker      <- ZIO.service[DefaultIngestionJobWorker]
      _           <- ZIO.logInfo("Starting ingestion job worker...")
      workerFiber <- worker.run.forkDaemon
      _           <- startGateway
      _           <- ZIO.never.onInterrupt(workerFiber.interrupt)
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

  private def ensureAllHealthy: ZIO[HealthCheckPort & RuntimeConfig, Throwable, Unit] =
    for {
      healthCheckPort <- ZIO.service[HealthCheckPort]
      _               <- ZIO.logInfo("Running health checks...")
      // One health-check attempt that fails if any dependency is unhealthy
      attempt = for {
                  results <- healthCheckPort.checkAllServices()
                  _       <- logHealthResults(results)
                  hasBad   = results.exists {
                             case com.cyrelis.linguapipe.application.types.HealthStatus.Healthy(_, _, _) => false
                             case _                                                                      => true
                           }
                  _ <- ZIO.when(hasBad)(
                         ZIO.fail(new RuntimeException("Unhealthy dependencies detected. Aborting startup."))
                       )
                } yield ()
      initial  = zio.Duration.fromMillis(2000)
      maxDelay = zio.Duration.fromMillis(10000)
      schedule = Schedule
                   .exponential(initial, 2.0)
                   .modifyDelay((_, d) => if (d > maxDelay) maxDelay else d)
                   .&&(Schedule.recurs(3))
      _ <- attempt.retry(schedule).catchAll { err =>
             ZIO.logError(err.getMessage) *> ZIO.fail(err)
           }
      _ <- ZIO.logInfo("All systems operational")
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

  private def startGateway: ZIO[Gateway & IngestPort & HealthCheckPort & RuntimeConfig & AllPorts, Throwable, Unit] =
    for {
      gateway <- ZIO.service[Gateway]
      _       <- gateway match {
             case restGateway: com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.IngestRestGateway =>
               restGateway.startWithDeps
             case _ =>
               gateway.start
           }
    } yield ()
}
