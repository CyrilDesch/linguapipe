package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import java.nio.file.Files
import java.util.{Base64, UUID}

import com.cyrelis.linguapipe.infrastructure.config.{AdapterFactory, RuntimeConfig}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.RichZEndpoint
import zio.*
import zio.http.*

object TestRoutes {

  // Test endpoints for individual adapters
  private val testTranscriberEndpoint
    : PublicEndpoint[TestTranscriberRestDto, String, TestTranscriberResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "transcriber")
      .in(multipartBody[TestTranscriberRestDto])
      .out(jsonBody[TestTranscriberResultRestDto])
      .errorOut(stringBody)
      .description("Test transcriber adapter with multipart audio file")

  private val testEmbedderEndpoint: PublicEndpoint[TestEmbedderRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "embedder")
      .in(jsonBody[TestEmbedderRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test embedder adapter with text")

  private val testBlobStoreEndpoint: PublicEndpoint[TestBlobStoreRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "blobstore")
      .in(jsonBody[TestBlobStoreRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test blob store adapter")

  private val testDocumentParserEndpoint: PublicEndpoint[TestDocumentParserRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "documentparser")
      .in(jsonBody[TestDocumentParserRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test document parser adapter")

  // Expose endpoints for Swagger/OpenAPI generation
  def endpoints: List[PublicEndpoint[?, ?, ?, ?]] =
    List(
      testTranscriberEndpoint,
      testEmbedderEndpoint,
      testBlobStoreEndpoint,
      testDocumentParserEndpoint
    )

  def createRoutes: ZIO[RuntimeConfig, Nothing, Routes[Any, Response]] =
    for {
      config <- ZIO.service[RuntimeConfig]
    } yield {
      // Create test adapters directly
      val transcriber    = AdapterFactory.createTranscriberAdapter(config.adapters.driven.transcriber)
      val embedder       = AdapterFactory.createEmbedderAdapter(config.adapters.driven.embedder)
      val blobStore      = AdapterFactory.createBlobStoreAdapter(config.adapters.driven.blobStore)
      val documentParser = AdapterFactory.createDocumentParserAdapter()

      ZioHttpInterpreter().toHttp(
        List(
          testTranscriberEndpoint.zServerLogic { req =>
            (for {
              format     <- ZIO.succeed(RestUtils.extractFormat(req.file))
              audioBytes <- ZIO.attempt(Files.readAllBytes(req.file.body.toPath))
              transcript <- transcriber.transcribe(audioBytes, format)
              result     <- ZIO.succeed(
                          TestTranscriberResultRestDto(
                            result = transcript.text
                          )
                        )
            } yield result).mapError(_.getMessage)
          },
          testEmbedderEndpoint.zServerLogic { req =>
            val transcript = com.cyrelis.linguapipe.domain.Transcript(
              id = UUID.randomUUID(),
              text = req.content,
              createdAt = java.time.Instant.now(),
              metadata = com.cyrelis.linguapipe.domain.TranscriptMetadata(
                source = com.cyrelis.linguapipe.domain.IngestSource.Text,
                attributes = Map.empty
              )
            )
            embedder
              .embed(transcript)
              .map(embedding =>
                TestResultRestDto(
                  result = s"${embedding.length} dimensions"
                )
              )
              .mapError(_.getMessage)
          },
          testBlobStoreEndpoint.zServerLogic { req =>
            (for {
              audioBytes <- ZIO.attempt(Base64.getDecoder.decode(req.content): Array[Byte])
              jobId      <- ZIO.succeed(UUID.randomUUID())
              _          <- blobStore.storeAudio(jobId, audioBytes, "wav")
              result     <- ZIO.succeed(
                          TestResultRestDto(
                            result = jobId.toString
                          )
                        )
            } yield result).mapError(_.getMessage)
          },
          testDocumentParserEndpoint.zServerLogic { req =>
            documentParser
              .parseDocument(req.content, "application/pdf")
              .map(text =>
                TestResultRestDto(
                  result = text.take(100)
                )
              )
              .mapError(_.getMessage)
          }
        )
      )
    }
}
