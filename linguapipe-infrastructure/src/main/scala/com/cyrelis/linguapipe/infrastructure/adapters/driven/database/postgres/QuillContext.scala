package com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres

import com.cyrelis.linguapipe.infrastructure.config.DatabaseAdapterConfig
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object QuillContext:

  def createLayer(
    config: DatabaseAdapterConfig.Postgres
  ): ZLayer[Any, Throwable, Quill.Postgres[SnakeCase]] =
    val dataSource = new org.postgresql.ds.PGSimpleDataSource()
    dataSource.setUrl(s"jdbc:postgresql://${config.host}:${config.port}/${config.database}")
    dataSource.setUser(config.user)
    dataSource.setPassword(config.password)

    Quill.DataSource.fromDataSource(dataSource) >>>
      Quill.Postgres.fromNamingStrategy(SnakeCase)
