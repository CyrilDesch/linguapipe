package com.cyrelis.linguapipe.application.usecase

import java.time.Instant
import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.*
import com.cyrelis.linguapipe.application.ports.driving.*
import com.cyrelis.linguapipe.domain.*
import zio.*

final class DefaultIngestPipeline(
  transcriber: TranscriberPort,
  embedder: EmbedderPort,
  dbSink: DbSinkPort,
  vectorSink: VectorSinkPort,
  blobStore: BlobStorePort,
  documentParser: DocumentParserPort
) extends IngestPort {

  override def executeAudio(audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Transcript] =
    for {
      transcript <- transcriber.transcribe(audioContent, format)
      _          <- dbSink.persistTranscript(transcript)
      embedding  <- embedder.embed(transcript)
      _          <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
    } yield transcript

  override def executeText(textContent: String): ZIO[Any, PipelineError, Transcript] =
    for {
      transcript <- ZIO.succeed(createTranscriptFromText(textContent))
      _          <- dbSink.persistTranscript(transcript)
      embedding  <- embedder.embed(transcript)
      _          <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
    } yield transcript

  override def executeDocument(documentContent: String, mediaType: String): ZIO[Any, PipelineError, Transcript] =
    for {
      jobId         <- ZIO.succeed(UUID.randomUUID())
      _             <- blobStore.storeDocument(jobId, documentContent, mediaType)
      extractedText <- documentParser.parseDocument(documentContent, mediaType)
      transcript    <- ZIO.succeed(createTranscriptFromDocument(extractedText, mediaType))
      _             <- dbSink.persistTranscript(transcript)
      embedding     <- embedder.embed(transcript)
      _             <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
    } yield transcript

  private def createTranscriptFromText(content: String): Transcript =
    Transcript(
      id = UUID.randomUUID(),
      language = None,
      text = content,
      confidence = 1.0, // Perfect confidence for text input
      createdAt = Instant.now(),
      source = IngestSource.Text,
      attributes = Map.empty
    )

  private def createTranscriptFromDocument(
    extractedText: String,
    mediaType: String
  ): Transcript =
    Transcript(
      id = UUID.randomUUID(),
      language = None,
      text = extractedText,
      confidence = 1.0, // Perfect confidence for document extraction
      createdAt = Instant.now(),
      source = IngestSource.Document,
      attributes = Map(
        "processing_method" -> "document_extraction",
        "media_type"        -> mediaType
      )
    )
}
