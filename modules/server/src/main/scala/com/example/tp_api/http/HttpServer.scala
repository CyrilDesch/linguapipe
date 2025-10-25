package com.example.tp.api.http

import zio.*
import zio.http.Server

import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import com.example.tp.api.http.prometheus.*
import com.example.tp.api.repositories.*
import com.example.tp.api.service.*
import com.example.tp.api.services.*

object HttpServer extends ZIOAppDefault {

  val serverOptions: ZioHttpServerOptions[Any] =
    ZioHttpServerOptions.customiseInterceptors
      .metricsInterceptor(metricsInterceptor)
      .appendInterceptor(
        CORSInterceptor.default
      )
      .options

  val runMigrations = for {
    flyway <- ZIO.service[FlywayService]
    _      <- flyway.runMigrations().catchSome { case e =>
           ZIO.logError(s"Error running migrations: ${e.getMessage()}")
             *> flyway.runRepair() *> flyway.runMigrations()
         }
  } yield ()

  private val server =
    for {
      _            <- Console.printLine("Starting server...")
      apiEndpoints <- HttpApi.endpoints
      docEndpoints  = SwaggerInterpreter()
                       .fromServerEndpoints(apiEndpoints, "tp_api", "1.0.0")
      allEndpoints   = (metricsEndpoint: ServerEndpoint[Any, Task]) :: (apiEndpoints ::: docEndpoints)
      httpApp        = ZioHttpInterpreter(serverOptions).toHttp(allEndpoints)
      _ <- Server.serve(httpApp)
    } yield ()

  private val program =
    for {
      _ <- runMigrations
      _ <- server
    } yield ()

  override def run =
    program
      .provide(
        Server.default,
        // Service layers
        PersonServiceLive.layer,
        FlywayServiceLive.configuredLayer,
        JWTServiceLive.configuredLayer,
        // Repository layers
        UserRepositoryLive.layer,
        PetRepositoryLive.layer,
        Repository.dataLayer
        // ,ZLayer.Debug.mermaid
      )
}
