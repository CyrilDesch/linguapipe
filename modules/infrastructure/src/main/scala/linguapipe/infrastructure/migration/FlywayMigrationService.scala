package linguapipe.infrastructure.migration

import org.flywaydb.core.Flyway
import zio.*

import linguapipe.infrastructure.config.DatabaseAdapterConfig

/**
 * Flyway-based database migration service. This is an infrastructure concern
 * that runs SQL migrations at application startup.
 */
trait MigrationService {
  def runMigrations(): Task[Unit]
}

final class FlywayMigrationService(config: DatabaseAdapterConfig) extends MigrationService {

  override def runMigrations(): Task[Unit] = {
    val (jdbcUrl, user, password) = config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        (
          s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.database}",
          cfg.user,
          cfg.password
        )
    }

    ZIO.attempt {
      val flyway = Flyway
        .configure()
        .dataSource(jdbcUrl, user, password)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()

      val migrationsApplied = flyway.migrate()

      migrationsApplied.migrationsExecuted
    }.flatMap { count =>
      if (count > 0) {
        ZIO.logInfo(s"✅ Applied $count database migration(s)")
      } else {
        ZIO.logInfo("✅ Database schema is up to date")
      }
    }.catchAll { error =>
      ZIO.logError(s"❌ Database migration failed: ${error.getMessage}") *>
        ZIO.fail(error)
    }
  }
}

object FlywayMigrationService {
  def layer: ZLayer[DatabaseAdapterConfig, Nothing, MigrationService] =
    ZLayer.fromFunction((config: DatabaseAdapterConfig) => new FlywayMigrationService(config))
}
