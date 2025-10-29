package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.TestRoutes
import com.cyrelis.linguapipe.infrastructure.config.RuntimeConfig
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.*

final class IngestRestGateway(
  host: String,
  port: Int
) extends Gateway {

  private def buildRoutes: ZIO[IngestPort & HealthCheckPort & RuntimeConfig, Nothing, Routes[Any, Response]] =
    for {
      mainRoutes <- MainRoutes.createRoutes
      testRoutes <- TestRoutes.createRoutes
      docsRoutes <- ZIO.succeed {
                      val docsEndpoints = SwaggerInterpreter(
                        swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("docs"))
                      ).fromEndpoints[Task](
                        MainRoutes.endpoints ++ TestRoutes.endpoints,
                        "LinguaPipe API",
                        "v1"
                      )
                      ZioHttpInterpreter().toHttp(docsEndpoints)
                    }
    } yield docsRoutes ++ mainRoutes ++ testRoutes

  override def start: ZIO[IngestPort & HealthCheckPort & RuntimeConfig, Throwable, Unit] =
    for {
      routes <- buildRoutes
      _      <- ZIO.logInfo(s"REST server will listen on $host:$port")
      _      <- {
        val url  = s"http://$host:$port/docs"
        val link = s"\u001B]8;;$url\u0007$url\u001B]8;;\u0007"
        ZIO.logInfo(s"REST server docs will be available at $link")
      }
      serverFiber <- Server
                       .serve(routes)
                       .provide(Server.defaultWithPort(port))
                       .fork
      _ <- serverFiber.await
    } yield ()

  override def description: String =
    s"REST API Gateway on $host:$port"
}
