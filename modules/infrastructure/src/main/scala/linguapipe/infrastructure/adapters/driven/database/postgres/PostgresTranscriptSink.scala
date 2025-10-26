package linguapipe.infrastructure.adapters.driven.database.postgres

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.DbSinkPort
import linguapipe.domain.{HealthStatus, Segment, Transcript}
import linguapipe.infrastructure.config.DatabaseAdapterConfig

/** PostgreSQL sink */
final class PostgresTranscriptSink(config: DatabaseAdapterConfig.Postgres) extends DbSinkPort {

  override def persistTranscript(transcript: Transcript): Task[Unit] =
    ZIO.succeed(
      println(s"[Postgres @ ${config.host}:${config.port}] Persist transcript ${transcript.id.value}")
    )

  override def persistSegments(segments: List[Segment]): Task[Unit] =
    ZIO.succeed(
      println(s"[Postgres @ ${config.host}:${config.port}] Persist ${segments.size} segments")
    )

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attempt {
      // In a real implementation, this would test the database connection
      // For now, we'll simulate a health check
      val isHealthy = true // This would be replaced with actual connection test

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
