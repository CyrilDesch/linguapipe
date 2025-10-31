package com.cyrelis.linguapipe.application.ports.driven.storage

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

trait VectorStorePort {
  def upsertEmbeddings(transcriptId: UUID, vectors: List[Array[Float]]): ZIO[Any, PipelineError, Unit]
  def healthCheck(): Task[HealthStatus]
}
