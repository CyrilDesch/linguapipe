package com.cyrelis.linguapipe.application.usecase

import java.util.UUID

import zio.*

import com.cyrelis.linguapipe.application.ports.driven.*
import com.cyrelis.linguapipe.application.ports.driving.*
import com.cyrelis.linguapipe.domain.*

final class DefaultIngestPipeline(
  transcriber: TranscriberPort,
  embedder: EmbedderPort,
  dbSink: DbSinkPort,
  vectorSink: VectorSinkPort,
  blobStore: BlobStorePort,
  documentParser: DocumentParserPort
) extends IngestPort {

  override def executeAudio(audioContent: String, format: String, language: Option[String]): Task[Transcript] =
    for {
      jobId      <- ZIO.succeed(UUID.randomUUID())
      _          <- blobStore.storeAudio(jobId, audioContent, format)
      transcript <- transcriber.transcribe(audioContent, format, language)
      _          <- dbSink.persistTranscript(transcript)
      embedding  <- embedder.embed(transcript)
      _          <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
    } yield transcript

  override def executeText(textContent: String, language: Option[String]): Task[Transcript] =
    for {
      transcript <- ZIO.succeed(createTranscriptFromText(textContent, language))
      _          <- dbSink.persistTranscript(transcript)
      embedding  <- embedder.embed(transcript)
      _          <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
    } yield transcript

  override def executeDocument(documentContent: String, mediaType: String, language: Option[String]): Task[Transcript] =
    for {
      jobId         <- ZIO.succeed(UUID.randomUUID())
      _             <- blobStore.storeDocument(jobId, documentContent, mediaType)
      extractedText <- documentParser.parseDocument(documentContent, mediaType)
      transcript    <- ZIO.succeed(createTranscriptFromDocument(extractedText, mediaType, language))
      _             <- dbSink.persistTranscript(transcript)
      embedding     <- embedder.embed(transcript)
      _             <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
    } yield transcript

  private def createTranscriptFromText(content: String, language: Option[String]): Transcript = {
    val metadata = TranscriptMetadata(
      source = IngestSource.Text,
      attributes = language.map(l => Map("language" -> l)).getOrElse(Map.empty)
    )
    Transcript.fromText(content, language, metadata)
  }

  private def createTranscriptFromDocument(
    extractedText: String,
    mediaType: String,
    language: Option[String]
  ): Transcript = {
    val metadata = TranscriptMetadata(
      source = IngestSource.Document,
      attributes = Map(
        "processing_method" -> "document_extraction",
        "media_type"        -> mediaType
      ) ++ language.map(l => ("language", l))
    )
    Transcript.fromText(extractedText, language, metadata)
  }
}
