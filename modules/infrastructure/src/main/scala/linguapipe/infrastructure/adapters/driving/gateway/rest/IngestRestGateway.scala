package linguapipe.infrastructure.adapters.driving.gateway.rest

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.RichZEndpoint
import zio.*
import zio.http.*
import zio.json.*

import linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import linguapipe.domain.*
import linguapipe.infrastructure.adapters.driving.Gateway
import linguapipe.infrastructure.adapters.driving.gateway.rest.{
  AudioIngestRestDto,
  DocumentIngestRestDto,
  IngestionResultRestDto,
  TextIngestRestDto
}

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

  private val ingestAudioEndpoint: PublicEndpoint[AudioIngestRestDto, String, IngestionResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "audio")
      .in(jsonBody[AudioIngestRestDto])
      .out(jsonBody[IngestionResultRestDto])
      .errorOut(stringBody)
      .description("Ingest base64-encoded audio content")

  private val ingestTextEndpoint: PublicEndpoint[TextIngestRestDto, String, IngestionResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "text")
      .in(jsonBody[TextIngestRestDto])
      .out(jsonBody[IngestionResultRestDto])
      .errorOut(stringBody)
      .description("Ingest raw text content")

  private val ingestDocumentEndpoint: PublicEndpoint[DocumentIngestRestDto, String, IngestionResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "document")
      .in(jsonBody[DocumentIngestRestDto])
      .out(jsonBody[IngestionResultRestDto])
      .errorOut(stringBody)
      .description("Ingest base64-encoded document content")

  private def buildRoutes: ZIO[IngestPort & HealthCheckPort, Nothing, Routes[Any, Response]] =
    for {
      ingestPort      <- ZIO.service[IngestPort]
      healthCheckPort <- ZIO.service[HealthCheckPort]
    } yield ZioHttpInterpreter().toHttp(
      List(
        healthEndpoint.zServerLogic(_ => healthCheckPort.checkAllServices().mapError(_.getMessage)),
        ingestAudioEndpoint.zServerLogic { req =>
          ingestPort
            .executeAudio(req.content, req.format, req.language)
            .map { transcript =>
              IngestionResultRestDto(
                transcriptId = transcript.id.toString,
                segmentsEmbedded = 1,
                completedAt = transcript.createdAt.toString
              )
            }
            .mapError(_.getMessage)
        },
        ingestTextEndpoint.zServerLogic { req =>
          ingestPort
            .executeText(req.content, req.language)
            .map { transcript =>
              IngestionResultRestDto(
                transcriptId = transcript.id.toString,
                segmentsEmbedded = 1,
                completedAt = transcript.createdAt.toString
              )
            }
            .mapError(_.getMessage)
        },
        ingestDocumentEndpoint.zServerLogic { req =>
          ingestPort
            .executeDocument(req.content, req.mediaType, req.language)
            .map { transcript =>
              IngestionResultRestDto(
                transcriptId = transcript.id.toString,
                segmentsEmbedded = 1,
                completedAt = transcript.createdAt.toString
              )
            }
            .mapError(_.getMessage)
        }
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
