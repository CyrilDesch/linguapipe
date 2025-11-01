package com.cyrelis.srag.application.ports.driven.embedding

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.HealthStatus
import com.cyrelis.srag.domain.transcript.Transcript
import zio.*

trait EmbedderPort {
  def embed(transcript: Transcript): ZIO[Any, PipelineError, List[(String, Array[Float])]]
  def embedQuery(query: String): ZIO[Any, PipelineError, Array[Float]]
  def healthCheck(): Task[HealthStatus]
}
