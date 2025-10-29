package com.cyrelis.linguapipe.application.ports.driven

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.Transcript
import zio.*

trait TranscriberPort {
  def transcribe(audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Transcript]
  def healthCheck(): Task[HealthStatus]
}

trait EmbedderPort {
  def embed(transcript: Transcript): ZIO[Any, PipelineError, Array[Float]]
  def healthCheck(): Task[HealthStatus]
}

trait DbSinkPort {
  def persistTranscript(transcript: Transcript): ZIO[Any, PipelineError, Unit]
  def getAllTranscripts(): ZIO[Any, PipelineError, List[Transcript]]
  def healthCheck(): Task[HealthStatus]
}

trait VectorSinkPort {
  def upsertEmbeddings(transcriptId: UUID, vectors: List[Array[Float]]): ZIO[Any, PipelineError, Unit]
  def healthCheck(): Task[HealthStatus]
}

trait BlobStorePort {
  def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Unit]
  def storeDocument(jobId: UUID, documentContent: String, mediaType: String): ZIO[Any, PipelineError, Unit]
  def healthCheck(): Task[HealthStatus]
}
