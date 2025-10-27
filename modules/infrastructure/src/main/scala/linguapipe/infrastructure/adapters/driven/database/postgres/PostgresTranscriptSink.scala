package linguapipe.infrastructure.adapters.driven.database.postgres

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.DbSinkPort
import linguapipe.domain.{HealthStatus, Transcript}
import linguapipe.infrastructure.config.DatabaseAdapterConfig

final class PostgresTranscriptSink(config: DatabaseAdapterConfig.Postgres) extends DbSinkPort {

  override def persistTranscript(transcript: Transcript): Task[Unit] =
    ZIO.succeed(
      println(
        s"[Postgres @ ${config.host}:${config.port}] Persist transcript ${transcript.id} with text: ${transcript.text.take(50)}..."
      )
    )

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      val isHealthy = true

      if (isHealthy) {
        HealthStatus.Healthy(
          serviceName = "PostgreSQL",
          checkedAt = Instant.now(),
          details = Map(
            "host"     -> config.host,
            "port"     -> config.port.toString,
            "database" -> config.database
          )
        )
      } else {
        HealthStatus.Unhealthy(
          serviceName = "PostgreSQL",
          checkedAt = Instant.now(),
          error = "Connection failed",
          details = Map(
            "host"     -> config.host,
            "port"     -> config.port.toString,
            "database" -> config.database
          )
        )
      }
    }
}
