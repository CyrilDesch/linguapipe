package linguapipe.infrastructure.adapters.driving.gateway.rest

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.RichZEndpoint
import zio.*
import zio.http.*

import linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import linguapipe.domain.*
import linguapipe.infrastructure.adapters.driving.Gateway

final class IngestRestGateway(
  host: String,
  port: Int
) extends Gateway {

  private val healthEndpoint: PublicEndpoint[Unit, String, List[HealthStatus], Any] =
    sttp.tapir.endpoint.get
      .in("health")
      .out(jsonBody[List[HealthStatus]])
      .errorOut(stringBody)
      .description("Health check endpoint")

  private val ingestEndpoint: PublicEndpoint[IngestCommand, String, IngestionResult, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest")
      .in(jsonBody[IngestCommand])
      .out(jsonBody[IngestionResult])
      .errorOut(stringBody)
      .description("Ingest audio/text/document content")

  private def buildRoutes: ZIO[IngestPort & HealthCheckPort, Nothing, Routes[Any, Response]] =
    for {
      ingestPort      <- ZIO.service[IngestPort]
      healthCheckPort <- ZIO.service[HealthCheckPort]
    } yield ZioHttpInterpreter().toHttp(
      List(
        healthEndpoint.zServerLogic(_ => healthCheckPort.checkAllServices().mapError(_.getMessage)),
        ingestEndpoint.zServerLogic(command => ingestPort.execute(command).mapError(_.getMessage))
      )
    )

  override def start: ZIO[IngestPort & HealthCheckPort, Throwable, Unit] =
    for {
      routes      <- buildRoutes
      _           <- ZIO.logInfo(s"REST server will listen on $host:$port")
      serverFiber <- Server
                       .serve(routes)
                       .provide(Server.defaultWithPort(port))
                       .fork
      _ <- serverFiber.await
    } yield ()

  override def description: String =
    s"REST API Gateway on $host:$port"
}
