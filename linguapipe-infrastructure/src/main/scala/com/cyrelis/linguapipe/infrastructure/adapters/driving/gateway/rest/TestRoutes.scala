package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import java.nio.file.Files
import java.util.{Base64, UUID}

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.DbSinkPort
import com.cyrelis.linguapipe.infrastructure.config.{AdapterFactory, RuntimeConfig}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.RichZEndpoint
import zio.*
import zio.http.*

object TestRoutes {

  private def errorToString(error: Throwable | PipelineError): String = error match {
    case pipelineError: PipelineError => pipelineError.message
    case throwable: Throwable         => throwable.getMessage
  }

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

  private val testDatabaseEndpoint: PublicEndpoint[TestDatabaseRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "database")
      .in(jsonBody[TestDatabaseRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test database adapter")

  private val testVectorStoreEndpoint: PublicEndpoint[TestVectorStoreRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "vectorstore")
      .in(jsonBody[TestVectorStoreRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test vector store adapter")

  private val getAllTranscriptsEndpoint: PublicEndpoint[Unit, String, List[TranscriptResponseDto], Any] =
    sttp.tapir.endpoint.get
      .in("test" / "transcripts")
      .out(jsonBody[List[TranscriptResponseDto]])
      .errorOut(stringBody)
      .description("Get all transcripts from database")

  // Expose endpoints for Swagger/OpenAPI generation
  def endpoints: List[PublicEndpoint[?, ?, ?, ?]] =
    List(
      testTranscriberEndpoint,
      testEmbedderEndpoint,
      testBlobStoreEndpoint,
      testDocumentParserEndpoint,
      testDatabaseEndpoint,
      testVectorStoreEndpoint,
      getAllTranscriptsEndpoint
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
      val vectorStore    = AdapterFactory.createVectorStoreAdapter(config.adapters.driven.vectorStore)
      // Database adapter requires ZLayer, so we'll create it inside the endpoint handlers
      val dbSinkLayer = AdapterFactory.createDatabaseAdapterLayer(config.adapters.driven.database)

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
            } yield result).mapError(errorToString)
          },
          testEmbedderEndpoint.zServerLogic { req =>
            val transcript = com.cyrelis.linguapipe.domain.Transcript(
              id = UUID.randomUUID(),
              language = None,
              text = req.content,
              confidence = 1.0,
              createdAt = java.time.Instant.now(),
              source = com.cyrelis.linguapipe.domain.IngestSource.Text,
              attributes = Map.empty
            )
            embedder
              .embed(transcript)
              .map(embedding =>
                TestResultRestDto(
                  result = s"${embedding.length} dimensions"
                )
              )
              .mapError(errorToString)
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
            } yield result).mapError(errorToString)
          },
          testDocumentParserEndpoint.zServerLogic { req =>
            documentParser
              .parseDocument(req.content, "application/pdf")
              .map(text =>
                TestResultRestDto(
                  result = text.take(100)
                )
              )
              .mapError(errorToString)
          },
          testDatabaseEndpoint.zServerLogic { req =>
            val domainSource = IngestSourceDto.toDomain(req.source)
            val transcript   = com.cyrelis.linguapipe.domain.Transcript(
              id = UUID.randomUUID(),
              language = None,
              text = req.text,
              confidence = 1.0,
              createdAt = java.time.Instant.now(),
              source = domainSource,
              attributes = Map.empty
            )
            (for {
              dbSink <- ZIO.service[DbSinkPort].provide(dbSinkLayer)
              _      <- dbSink.persistTranscript(transcript)
              result <- ZIO.succeed(
                          TestResultRestDto(
                            result = s"Persisted transcript ${transcript.id}"
                          )
                        )
            } yield result).mapError(errorToString)
          },
          testVectorStoreEndpoint.zServerLogic { req =>
            (for {
              transcriptId <-
                ZIO
                  .attempt(UUID.fromString(req.transcriptId))
                  .mapError(e => PipelineError.VectorStoreError(s"Invalid UUID: ${req.transcriptId}", Some(e)))
              vectors = req.vectors.map(_.toArray)
              _      <- vectorStore.upsertEmbeddings(transcriptId, vectors)
              result <- ZIO.succeed(
                          TestResultRestDto(
                            result = s"Upserted ${vectors.size} vectors for transcript $transcriptId"
                          )
                        )
            } yield result).mapError(errorToString)
          },
          getAllTranscriptsEndpoint.zServerLogic { _ =>
            (for {
              dbSink      <- ZIO.service[DbSinkPort].provide(dbSinkLayer)
              transcripts <- dbSink.getAllTranscripts()
              result      <- ZIO.succeed(
                          transcripts.map(TranscriptResponseDto.fromDomain)
                        )
            } yield result).mapError(errorToString)
          }
        )
      )
    }
}
