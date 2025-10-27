package linguapipe.application.usecase

import java.time.Instant

import zio.*

import linguapipe.application.ports.driven.*
import linguapipe.application.ports.driving.*
import linguapipe.domain.*

final class DefaultIngestPipeline(
  transcriber: TranscriberPort,
  embedder: EmbedderPort,
  dbSink: DbSinkPort,
  vectorSink: VectorSinkPort,
  blobStore: BlobStorePort,
  documentParser: DocumentParserPort
) extends IngestPort {

  override def execute(command: IngestCommand): Task[IngestionResult] =
    command.payload match {
      case IngestPayload.Base64Audio(content, format, language) =>
        executeAudioIngestion(command, content, format, language)
      case IngestPayload.InlineText(content, language) =>
        executeTextIngestion(command, content, language)
      case IngestPayload.Base64Document(content, mediaType, language) =>
        executeDocumentIngestion(command, content, mediaType, language)
    }

  private def executeAudioIngestion(
    command: IngestCommand,
    content: String,
    format: String,
    language: Option[String]
  ): Task[IngestionResult] =
    for {
      _           <- blobStore.store(command.jobId, command.payload)
      transcript  <- transcriber.transcribe(IngestPayload.Base64Audio(content, format, language))
      _           <- dbSink.persistTranscript(transcript)
      embedding   <- embedder.embed(transcript)
      _           <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
      completedAt <- ZIO.succeed(Instant.now())
    } yield IngestionResult(transcript.id, 1, completedAt)

  private def executeTextIngestion(
    command: IngestCommand,
    content: String,
    language: Option[String]
  ): Task[IngestionResult] =
    for {
      _           <- blobStore.store(command.jobId, command.payload)
      transcript   = Transcript.fromText(content, language, command.metadata)
      _           <- dbSink.persistTranscript(transcript)
      embedding   <- embedder.embed(transcript)
      _           <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
      completedAt <- ZIO.succeed(Instant.now())
    } yield IngestionResult(transcript.id, 1, completedAt)

  private def executeDocumentIngestion(
    command: IngestCommand,
    content: String,
    mediaType: String,
    language: Option[String]
  ): Task[IngestionResult] =
    for {
      _             <- blobStore.store(command.jobId, command.payload)
      extractedText <- documentParser.parseDocument(IngestPayload.Base64Document(content, mediaType, language))
      transcript     = Transcript.fromText(
                     extractedText,
                     language,
                     command.metadata.copy(
                       source = IngestSource.Document,
                       attributes = command.metadata.attributes ++ Map(
                         "processing_method" -> "document_extraction",
                         "media_type"        -> mediaType
                       )
                     )
                   )
      _           <- dbSink.persistTranscript(transcript)
      embedding   <- embedder.embed(transcript)
      _           <- vectorSink.upsertEmbeddings(transcript.id, List(embedding))
      completedAt <- ZIO.succeed(Instant.now())
    } yield IngestionResult(transcript.id, 1, completedAt)

}
