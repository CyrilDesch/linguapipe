package linguapipe.application.ports.driven

import java.util.UUID

import zio.*

import linguapipe.domain.{HealthStatus, IngestPayload, *}

trait TranscriberPort {
  def transcribe(payload: IngestPayload): Task[Transcript]
  def healthCheck(): Task[HealthStatus]
}

trait EmbedderPort {
  def embed(segment: Segment): Task[Array[Float]]
  def healthCheck(): Task[HealthStatus]
}

trait DbSinkPort {
  def persistTranscript(transcript: Transcript): Task[Unit]
  def persistSegments(segments: List[Segment]): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}

trait VectorSinkPort {
  def upsertEmbeddings(transcriptId: UUID, vectors: List[(UUID, Array[Float])]): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}

trait BlobStorePort {
  def store(jobId: UUID, payload: IngestPayload): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}
