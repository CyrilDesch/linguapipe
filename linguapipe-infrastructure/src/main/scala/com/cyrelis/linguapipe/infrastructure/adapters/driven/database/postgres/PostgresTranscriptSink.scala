package com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres

import java.time.Instant

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.DbSinkPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.Transcript
import com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres.models.TranscriptRow
import com.cyrelis.linguapipe.infrastructure.config.DatabaseAdapterConfig
import com.cyrelis.linguapipe.infrastructure.resilience.ErrorMapper
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

final case class HealthCheckResult(result: Int)

final class PostgresTranscriptSink(
  config: DatabaseAdapterConfig.Postgres,
  ctx: Quill.Postgres[SnakeCase]
) extends DbSinkPort:

  import ctx.*

  private inline def transcripts = quote(querySchema[TranscriptRow]("transcripts"))

  override def persistTranscript(transcript: Transcript): ZIO[Any, PipelineError, Unit] =
    ErrorMapper.mapDatabaseError {
      val transcriptRow = TranscriptRow.fromTranscript(transcript)

      inline def insertTranscript = quote {
        transcripts.insertValue(lift(transcriptRow))
      }

      ctx.run(insertTranscript) *> ZIO.logDebug(
        s"Persisted transcript ${transcript.id} to PostgreSQL"
      )
    }

  override def getAllTranscripts(): ZIO[Any, PipelineError, List[Transcript]] =
    ErrorMapper.mapDatabaseError {
      inline def getAllTranscriptsQuery = quote {
        transcripts.sortBy(_.createdAt)(using Ord.desc)
      }

      ctx.run(getAllTranscriptsQuery).map(rows => rows.map(TranscriptRow.toTranscript))
    }

  override def healthCheck(): Task[HealthStatus] =
    ErrorMapper.mapDatabaseError {
      inline def healthCheckQuery = quote {
        infix"SELECT 1 AS result".as[Query[HealthCheckResult]]
      }
      ctx.run(healthCheckQuery)
    }.map { _ =>
      HealthStatus.Healthy(
        serviceName = "PostgreSQL",
        checkedAt = Instant.now(),
        details = Map(
          "host"     -> config.host,
          "port"     -> config.port.toString,
          "database" -> config.database
        )
      )
    }.catchAll { error =>
      ZIO.succeed(
        HealthStatus.Unhealthy(
          serviceName = "PostgreSQL",
          checkedAt = Instant.now(),
          error = error.message,
          details = Map(
            "host"     -> config.host,
            "port"     -> config.port.toString,
            "database" -> config.database
          )
        )
      )
    }
