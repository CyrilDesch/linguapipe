package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.domain.ingestionjob.IngestionJobRepository
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import com.cyrelis.linguapipe.infrastructure.adapters.driving.Gateway
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.endpoint.{MainEndpoints, TestEndpoints}
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.handler.{MainHandlers, TestHandlers}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.RichZEndpoint
import zio.*
import zio.http.*

final class IngestRestGateway(
  host: String,
  port: Int
) extends Gateway {

  type TestEnv = TranscriberPort & EmbedderPort & BlobStorePort & DocumentParserPort &
    TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]] & VectorStorePort &
    IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]]

  private def buildRoutes: ZIO[IngestPort & HealthCheckPort & TestEnv, Nothing, Routes[Any, Response]] =
    for {
      mainRoutes <- buildMainRoutes
      testRoutes <- buildTestRoutes
      docsRoutes <- buildDocsRoutes
    } yield docsRoutes ++ mainRoutes ++ testRoutes

  private def buildMainRoutes: ZIO[
    IngestPort & HealthCheckPort & BlobStorePort & TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]],
    Nothing,
    Routes[Any, Response]
  ] =
    for {
      ingestPort      <- ZIO.service[IngestPort]
      healthCheckPort <- ZIO.service[HealthCheckPort]
      blobStore       <- ZIO.service[BlobStorePort]
      transcriptRepo  <- ZIO.service[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]]
    } yield ZioHttpInterpreter().toHttp(
      List(
        MainEndpoints.health.zServerLogic(_ => MainHandlers.handleHealth.provide(ZLayer.succeed(healthCheckPort))),
        MainEndpoints.ingestAudioMultipart.zServerLogic(
          MainHandlers.handleIngestAudio(_).provide(ZLayer.succeed(ingestPort))
        ),
        MainEndpoints.ingestText.zServerLogic(MainHandlers.handleIngestText(_).provide(ZLayer.succeed(ingestPort))),
        MainEndpoints.ingestDocument.zServerLogic(
          MainHandlers.handleIngestDocument(_).provide(ZLayer.succeed(ingestPort))
        ),
        MainEndpoints.jobStatus.zServerLogic(
          MainHandlers.handleGetJobStatus(_).provide(ZLayer.succeed(ingestPort))
        ),
        MainEndpoints.transcripts.zServerLogic { case (q, queryParams) =>
          val qpMap                                  = queryParams.toMap
          val filtersFromParams: Map[String, String] = qpMap.collect {
            case (k, v) if k.startsWith("metadata.") =>
              k.stripPrefix("metadata.") -> v
          }
          val filtersFromList: Map[String, String] = q.metadata.flatMap { entry =>
            val idx = entry.indexOf('=')
            if (idx > 0) Some(entry.substring(0, idx) -> entry.substring(idx + 1)) else None
          }.toMap
          val filters      = filtersFromParams ++ filtersFromList
          val sortBy       = q.sortBy
          val metadataSort = q.metadataSort
          val order        = q.order
          MainHandlers
            .handleGetTranscripts(filters, sortBy, metadataSort, order)
            .provide(ZLayer.succeed(transcriptRepo))
        },
        MainEndpoints.getFile.zServerLogic(
          MainHandlers.handleGetFile(_).provide(ZLayer.succeed(blobStore))
        )
      )
    )

  private def buildTestRoutes: ZIO[TestEnv, Nothing, Routes[Any, Response]] =
    for {
      transcriber    <- ZIO.service[TranscriberPort]
      embedder       <- ZIO.service[EmbedderPort]
      blobStore      <- ZIO.service[BlobStorePort]
      documentParser <- ZIO.service[DocumentParserPort]
      dbSink         <- ZIO.service[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]]
      vectorSink     <- ZIO.service[VectorStorePort]
    } yield ZioHttpInterpreter().toHttp(
      List(
        TestEndpoints.testTranscriber.zServerLogic(
          TestHandlers.handleTranscriber(_).provide(ZLayer.succeed(transcriber))
        ),
        TestEndpoints.testEmbedder.zServerLogic(TestHandlers.handleEmbedder(_).provide(ZLayer.succeed(embedder))),
        TestEndpoints.testBlobStore.zServerLogic(TestHandlers.handleBlobStore(_).provide(ZLayer.succeed(blobStore))),
        TestEndpoints.testDocumentParser.zServerLogic(
          TestHandlers.handleDocumentParser(_).provide(ZLayer.succeed(documentParser))
        ),
        TestEndpoints.testDatabase.zServerLogic(TestHandlers.handleDatabase(_).provide(ZLayer.succeed(dbSink))),
        TestEndpoints.testVectorStore.zServerLogic(
          TestHandlers.handleVectorStore(_).provide(ZLayer.succeed(vectorSink))
        ),
        TestEndpoints.getAllTranscripts.zServerLogic(_ =>
          TestHandlers.handleGetAllTranscripts.provide(ZLayer.succeed(dbSink))
        )
      )
    )

  private def buildDocsRoutes: ZIO[Any, Nothing, Routes[Any, Response]] =
    ZIO.succeed {
      val docsEndpoints = SwaggerInterpreter(
        swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("docs"))
      ).fromEndpoints[Task](
        MainEndpoints.all ++ TestEndpoints.all,
        "LinguaPipe API",
        "v1"
      )
      ZioHttpInterpreter().toHttp(docsEndpoints)
    }

  def startWithDeps: ZIO[IngestPort & HealthCheckPort & TestEnv, Throwable, Unit] =
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

  override def start: ZIO[Any, Throwable, Unit] =
    ZIO.logWarning("Gateway.start() called without dependencies - this should not happen in normal operation")

  override def description: String =
    s"REST API Gateway on $host:$port"
}
