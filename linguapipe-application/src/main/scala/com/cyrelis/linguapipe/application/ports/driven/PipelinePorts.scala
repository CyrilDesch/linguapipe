package com.cyrelis.linguapipe.application.ports.driven

import java.util.UUID

import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.Transcript
import zio.*

trait TranscriberPort {
  def transcribe(audioContent: Array[Byte], format: String): Task[Transcript]
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
  def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): Task[Unit]
  def storeDocument(jobId: UUID, documentContent: String, mediaType: String): Task[Unit]
  def healthCheck(): Task[HealthStatus]
}
