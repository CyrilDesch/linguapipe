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
  blobStore: BlobStorePort
) extends IngestPort {

  override def execute(command: IngestCommand): Task[IngestionResult] =
    for {
      _           <- blobStore.store(command.jobId, command.payload)
      transcript  <- transcriber.transcribe(command.payload)
      segments    <- ZIO.succeed(transcript.segments)
      _           <- dbSink.persistTranscript(transcript)
      _           <- dbSink.persistSegments(segments)
      embeddings  <- ZIO.foreach(segments)(s => embedder.embed(s).map(vec => s.id -> vec))
      _           <- vectorSink.upsertEmbeddings(transcript.id, embeddings)
      completedAt <- ZIO.succeed(Instant.now())
    } yield IngestionResult(transcript.id, embeddings.size, completedAt)
}
