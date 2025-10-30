package com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres

import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.config.DatabaseAdapterConfig
import zio.*
import zio.test.*

object PostgresDatasourceTest extends ZIOSpecDefault:

  private val invalidConfig: DatabaseAdapterConfig.Postgres = DatabaseAdapterConfig.Postgres(
    host = "nonexistent-host",
    port = 9999,
    database = "nonexistent_db",
    user = "test_user",
    password = "test_password"
  )

  def spec = suite("PostgresDatasource health checks")(
    test("should return Unhealthy for unreachable database datasource") {
      for {
        datasource <- ZIO.service[PostgresDatasource].provide(PostgresDatasource.layer(invalidConfig))
        status     <- datasource.healthCheck()
      } yield assertTrue(
        status match {
          case HealthStatus.Unhealthy(serviceName, _, error, details) =>
            serviceName == "PostgreSQL" &&
            error.nonEmpty &&
            details.contains("host") &&
            details("host") == invalidConfig.host &&
            details.contains("port") &&
            details("port") == invalidConfig.port.toString &&
            details.contains("database") &&
            details("database") == invalidConfig.database
          case _ => false
        }
      )
    },
    test("should include correct service name in health status") {
      for {
        datasource <- ZIO.service[PostgresDatasource].provide(PostgresDatasource.layer(invalidConfig))
        status     <- datasource.healthCheck()
      } yield {
        val serviceName = status match {
          case HealthStatus.Healthy(name, _, _)      => name
          case HealthStatus.Unhealthy(name, _, _, _) => name
          case HealthStatus.Timeout(name, _, _)      => name
        }
        assertTrue(serviceName == "PostgreSQL")
      }
    },
    test("should include database connection details in unhealthy status") {
      for {
        datasource <- ZIO.service[PostgresDatasource].provide(PostgresDatasource.layer(invalidConfig))
        status     <- datasource.healthCheck()
      } yield {
        status match {
          case HealthStatus.Unhealthy(_, _, _, details) =>
            assertTrue(
              details.contains("host") &&
                details.contains("port") &&
                details.contains("database") &&
                details("host") == invalidConfig.host &&
                details("port") == invalidConfig.port.toString &&
                details("database") == invalidConfig.database
            )
          case _ => assertTrue(false)
        }
      }
    }
  )
