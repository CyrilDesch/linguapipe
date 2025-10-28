package linguapipe.application.ports.driven

import java.util.UUID

import zio.*

import linguapipe.domain.{HealthStatus, Transcript}

trait TranscriberPort {
  def transcribe(audioContent: String, format: String, language: Option[String]): Task[Transcript]
  def healthCheck(): Task[HealthStatus]
}

trait EmbedderPort {
  def embed(transcript: Transcript): Task[Array[Float]]
  def healthCheck(): Task[HealthStatus]
}

trait DbSinkPort {
  def persistTranscript(transcript: Transcript): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}

trait VectorSinkPort {
  def upsertEmbeddings(transcriptId: UUID, vectors: List[Array[Float]]): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}

trait BlobStorePort {
  def storeAudio(jobId: UUID, audioContent: String, format: String): Task[Unit]
  def storeDocument(jobId: UUID, documentContent: String, mediaType: String): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}
