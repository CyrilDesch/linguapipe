package com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres

import java.time.Instant

import com.cyrelis.linguapipe.application.ports.driven.datasource.DatasourcePort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.config.DatabaseAdapterConfig
import com.cyrelis.linguapipe.infrastructure.resilience.ErrorMapper
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

final case class HealthCheckResult(result: Int)

trait QuillDatasource extends DatasourcePort {
  def quillContext: Quill.Postgres[SnakeCase]
}

object PostgresDatasource {
  def layer(config: DatabaseAdapterConfig.Postgres): ZLayer[Any, Throwable, PostgresDatasource] =
    val dataSource = new org.postgresql.ds.PGSimpleDataSource()
    dataSource.setUrl(s"jdbc:postgresql://${config.host}:${config.port}/${config.database}")
    dataSource.setUser(config.user)
    dataSource.setPassword(config.password)

    Quill.DataSource.fromDataSource(dataSource) >>>
      Quill.Postgres.fromNamingStrategy(SnakeCase) >>>
      ZLayer.fromFunction((quill: Quill.Postgres[SnakeCase]) => new PostgresDatasource(config, quill))
}

final class PostgresDatasource(
  config: DatabaseAdapterConfig.Postgres,
  val quillContext: Quill.Postgres[SnakeCase]
) extends QuillDatasource {

  override def healthCheck(): Task[HealthStatus] =
    ErrorMapper.mapDatabaseError {
      import quillContext.*
      inline def healthCheckQuery = quote {
        infix"SELECT 1 AS result".as[Query[HealthCheckResult]]
      }
      quillContext.run(healthCheckQuery)
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
}
