package com.cyrelis.linguapipe.application.ports.driven.embedding

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.transcript.Transcript
import zio.*

trait EmbedderPort {
  def embed(transcript: Transcript): ZIO[Any, PipelineError, List[(String, Array[Float])]]
  def embedQuery(query: String): ZIO[Any, PipelineError, Array[Float]]
  def healthCheck(): Task[HealthStatus]
}
