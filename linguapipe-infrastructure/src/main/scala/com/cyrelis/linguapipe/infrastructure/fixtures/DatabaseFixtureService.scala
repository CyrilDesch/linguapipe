package com.cyrelis.linguapipe.infrastructure.fixtures

import com.cyrelis.linguapipe.infrastructure.config.DatabaseAdapterConfig
import zio.*

trait FixtureService {
  def loadFixtures(): Task[Unit]
  def hasData(): Task[Boolean]
}

final class DatabaseFixtureService(config: DatabaseAdapterConfig) extends FixtureService {

  override def hasData(): Task[Boolean] = {
    val (jdbcUrl, user, password) = config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        (
          s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.database}",
          cfg.user,
          cfg.password
        )
    }

    ZIO.attempt {
      import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}

      val connection: Connection = DriverManager.getConnection(jdbcUrl, user, password)
      try {
        val statement: PreparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM transcripts")
        try {
          val resultSet: ResultSet = statement.executeQuery()
          try {
            if (resultSet.next()) {
              resultSet.getInt(1) > 0
            } else {
              false
            }
          } finally {
            resultSet.close()
          }
        } finally {
          statement.close()
        }
      } finally {
        connection.close()
      }
    }.catchAll { error =>
      ZIO.logWarning(s"Could not check if data exists: ${error.getMessage}") *>
        ZIO.succeed(false)
    }
  }

  override def loadFixtures(): Task[Unit] = {
    val (jdbcUrl, user, password) = config match {
      case cfg: DatabaseAdapterConfig.Postgres =>
        (
          s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.database}",
          cfg.user,
          cfg.password
        )
    }

    ZIO.attempt {
      import java.sql.{Connection, DriverManager, Statement}
      import java.nio.file.{Files, Paths}

      val connection: Connection = DriverManager.getConnection(jdbcUrl, user, password)
      try {
        val fixturePath  = Paths.get(getClass.getClassLoader.getResource("db/fixtures").toURI)
        val fixtureFiles = Files
          .list(fixturePath)
          .filter(_.toString.endsWith(".sql"))
          .sorted()
          .toArray()
          .map(_.toString)

        for (filePath <- fixtureFiles) {
          val sql                  = Files.readString(Paths.get(filePath))
          val statement: Statement = connection.createStatement()
          try {
            statement.execute(sql)
          } finally {
            statement.close()
          }
        }
      } finally {
        connection.close()
      }
    }.flatMap { _ =>
      ZIO.logInfo("Database fixtures loaded successfully")
    }.catchAll { error =>
      ZIO.logError(s"Failed to load database fixtures: ${error.getMessage}") *>
        ZIO.fail(error)
    }
  }
}

object DatabaseFixtureService {
  def layer: ZLayer[DatabaseAdapterConfig, Nothing, FixtureService] =
    ZLayer.fromFunction((config: DatabaseAdapterConfig) => new DatabaseFixtureService(config))
}
