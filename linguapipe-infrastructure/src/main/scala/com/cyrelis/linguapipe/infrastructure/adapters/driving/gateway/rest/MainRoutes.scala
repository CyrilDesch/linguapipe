package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import java.nio.file.Files

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import sttp.model.Part
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.ztapir.RichZEndpoint
import zio.*
import zio.http.*

object MainRoutes {

  private def errorToString(error: Throwable | PipelineError): String = error match {
    case pipelineError: PipelineError => pipelineError.message
    case throwable: Throwable         => throwable.getMessage
  }

  private val healthEndpoint: PublicEndpoint[Unit, String, List[HealthStatusRestDto], Any] =
    sttp.tapir.endpoint.get
      .in("health")
      .out(jsonBody[List[HealthStatusRestDto]])
      .errorOut(stringBody)
      .description("Health check endpoint")

  private val ingestAudioMultipartEndpoint
    : PublicEndpoint[AudioIngestMultipartDto, String, IngestionResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "audio")
      .in(multipartBody[AudioIngestMultipartDto])
      .out(jsonBody[IngestionResultRestDto])
      .errorOut(stringBody)
      .description("Ingest audio file directly (multipart/form-data)")

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

  // Expose endpoints for Swagger/OpenAPI generation
  def endpoints: List[PublicEndpoint[?, ?, ?, ?]] =
    List(
      healthEndpoint,
      ingestAudioMultipartEndpoint,
      ingestTextEndpoint,
      ingestDocumentEndpoint
    )

  def createRoutes: ZIO[IngestPort & HealthCheckPort, Nothing, Routes[Any, Response]] =
    for {
      ingestPort      <- ZIO.service[IngestPort]
      healthCheckPort <- ZIO.service[HealthCheckPort]
    } yield sttp.tapir.server.ziohttp
      .ZioHttpInterpreter()
      .toHttp(
        List(
          healthEndpoint.zServerLogic(_ =>
            healthCheckPort
              .checkAllServices()
              .map(_.map(HealthStatusRestDto.fromApplication))
              .mapError(_.getMessage)
          ),
          ingestAudioMultipartEndpoint.zServerLogic { req =>
            (for {
              format     <- ZIO.succeed(RestUtils.extractFormat(req.file))
              audioBytes <- ZIO.attempt(Files.readAllBytes(req.file.body.toPath))
              transcript <- ingestPort
                              .executeAudio(audioBytes, format)
              result <- ZIO.succeed(
                          IngestionResultRestDto(
                            transcriptId = transcript.id.toString,
                            segmentsEmbedded = 1,
                            completedAt = transcript.createdAt.toString
                          )
                        )
            } yield result).mapError(errorToString)
          },
          ingestTextEndpoint.zServerLogic { req =>
            ingestPort
              .executeText(req.content)
              .map { transcript =>
                IngestionResultRestDto(
                  transcriptId = transcript.id.toString,
                  segmentsEmbedded = 1,
                  completedAt = transcript.createdAt.toString
                )
              }
              .mapError(errorToString)
          },
          ingestDocumentEndpoint.zServerLogic { req =>
            ingestPort
              .executeDocument(req.content, req.mediaType)
              .map { transcript =>
                IngestionResultRestDto(
                  transcriptId = transcript.id.toString,
                  segmentsEmbedded = 1,
                  completedAt = transcript.createdAt.toString
                )
              }
              .mapError(errorToString)
          }
        )
      )

}
