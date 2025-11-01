package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.handler

import java.nio.file.Files
import java.util.UUID

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.srag.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.srag.application.ports.driven.storage.{BlobStorePort, VectorStorePort}
import com.cyrelis.srag.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.srag.domain.transcript.{IngestSource, Transcript, TranscriptRepository}
import com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.common.IngestSourceDto
import com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test.*
import com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.error.ErrorHandler
import zio.*

object TestHandlers {

  def handleTranscriber(req: TestTranscriberRestDto): ZIO[TranscriberPort, String, TestTranscriberResultRestDto] =
    (for {
      contentType <- ZIO
                       .fromOption(req.file.contentType.map(_.toString))
                       .orElseFail(PipelineError.ConfigurationError("Missing Content-Type header in multipart request"))
      fileName <- ZIO
                    .fromOption(req.file.fileName)
                    .orElseFail(PipelineError.ConfigurationError("Missing filename in multipart request"))
      audioBytes <- ZIO.attempt(Files.readAllBytes(req.file.body.toPath))
      transcript <- ZIO.serviceWithZIO[TranscriberPort](_.transcribe(audioBytes, contentType, fileName))
      result     <- ZIO.succeed(
                  TestTranscriberResultRestDto(
                    result = transcript.text
                  )
                )
    } yield result).mapError(ErrorHandler.errorToString)

  def handleEmbedder(req: TestEmbedderRestDto): ZIO[EmbedderPort, String, TestEmbedderResultRestDto] = {
    val transcript = Transcript(
      id = UUID.randomUUID(),
      language = None,
      text = req.content,
      confidence = 1.0,
      createdAt = java.time.Instant.now(),
      source = IngestSource.Text,
      metadata = Map.empty
    )
    ZIO
      .serviceWithZIO[EmbedderPort](_.embed(transcript))
      .map { segments =>
        TestEmbedderResultRestDto(
          totalChunks = segments.size,
          chunks = segments.map { case (chunk, embedding) =>
            EmbeddingChunkDto(
              chunk = chunk,
              dimensions = embedding.length,
              embedding = embedding.toList
            )
          }
        )
      }
      .mapError(ErrorHandler.errorToString)
  }

  def handleBlobStore(req: TestBlobStoreRestDto): ZIO[BlobStorePort, String, TestResultRestDto] =
    (for {
      contentType <- ZIO
                       .fromOption(req.file.contentType.map(_.toString))
                       .orElseFail(PipelineError.ConfigurationError("Missing Content-Type header in multipart request"))
      fileName <- ZIO
                    .fromOption(req.file.fileName)
                    .orElseFail(PipelineError.ConfigurationError("Missing filename in multipart request"))
      audioBytes <- ZIO.attempt(Files.readAllBytes(req.file.body.toPath))
      jobId      <- ZIO.succeed(UUID.randomUUID())
      key        <- ZIO.serviceWithZIO[BlobStorePort](_.storeAudio(jobId, audioBytes, contentType, fileName))
      result     <- ZIO.succeed(
                  TestResultRestDto(
                    result = s"Stored under key $key"
                  )
                )
    } yield result).mapError(ErrorHandler.errorToString)

  def handleDocumentParser(req: TestDocumentParserRestDto): ZIO[DocumentParserPort, String, TestResultRestDto] =
    ZIO
      .serviceWithZIO[DocumentParserPort](_.parseDocument(req.content, "application/pdf"))
      .map(text =>
        TestResultRestDto(
          result = text.take(100)
        )
      )
      .mapError(ErrorHandler.errorToString)

  def handleDatabase(
    req: TestDatabaseRestDto
  ): ZIO[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]], String, TestResultRestDto] = {
    val domainSource = IngestSourceDto.toDomain(req.source)
    val transcript   = Transcript(
      id = UUID.randomUUID(),
      language = None,
      text = req.text,
      confidence = 1.0,
      createdAt = java.time.Instant.now(),
      source = domainSource,
      metadata = Map.empty
    )
    (for {
      _      <- ZIO.serviceWithZIO[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]](_.persist(transcript))
      result <- ZIO.succeed(
                  TestResultRestDto(
                    result = s"Persisted transcript ${transcript.id}"
                  )
                )
    } yield result).mapError(ErrorHandler.errorToString)
  }

  def handleVectorStore(
    req: TestVectorStoreRestDto
  ): ZIO[VectorStorePort & EmbedderPort, String, TestResultRestDto] =
    (for {
      transcriptId <- ZIO.succeed(UUID.randomUUID())
      transcript    = Transcript(
                     id = transcriptId,
                     language = None,
                     text = req.text,
                     confidence = 1.0,
                     createdAt = java.time.Instant.now(),
                     source = IngestSource.Text,
                     metadata = Map.empty
                   )
      embeddings <- ZIO.serviceWithZIO[EmbedderPort](_.embed(transcript))
      vectors     = embeddings.map(_._2)
      // Temporairement: ajouter le texte du transcript dans les métadonnées
      metadata = Map("transcript_text" -> transcript.text)
      _       <- ZIO.serviceWithZIO[VectorStorePort](_.upsertEmbeddings(transcriptId, vectors, metadata))
      result  <- ZIO.succeed(
                  TestResultRestDto(
                    result = s"Upserted ${vectors.size} vectors for transcript $transcriptId"
                  )
                )
    } yield result).mapError(ErrorHandler.errorToString)

  def handleVectorStoreQuery(
    req: TestVectorStoreQueryRestDto
  ): ZIO[VectorStorePort & EmbedderPort, String, TestVectorStoreQueryResultRestDto] =
    (for {
      queryVector   <- ZIO.serviceWithZIO[EmbedderPort](_.embedQuery(req.text))
      limit          = 5
      searchResults <- ZIO.serviceWithZIO[VectorStorePort](_.searchSimilar(queryVector, limit, None))
      result        <- ZIO.succeed(
                  TestVectorStoreQueryResultRestDto(
                    results = searchResults.map(r =>
                      VectorSearchResultDto(
                        transcriptId = r.transcriptId.toString,
                        segmentIndex = r.segmentIndex,
                        score = r.score,
                        transcriptText =
                          r.metadata.flatMap(_.get("transcript_text")) // Temporairement: extraire le texte du payload
                      )
                    ),
                    totalResults = searchResults.size
                  )
                )
    } yield result).mapError(ErrorHandler.errorToString)

  def handleGetAllTranscripts
    : ZIO[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]], String, List[TranscriptResponseDto]] =
    ZIO
      .serviceWithZIO[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]](_.getAll())
      .map(_.map(TranscriptResponseDto.fromDomain))
      .mapError(ErrorHandler.errorToString)
}
