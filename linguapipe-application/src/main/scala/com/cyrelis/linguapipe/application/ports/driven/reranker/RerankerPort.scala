package com.cyrelis.linguapipe.application.ports.driven.reranker

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.{HealthStatus, RerankerCandidate, RerankerResult}
import zio.*

trait RerankerPort {
  def rerank(
    query: String,
    candidates: List[RerankerCandidate],
    topK: Int
  ): ZIO[Any, PipelineError, List[RerankerResult]]

  def healthCheck(): Task[HealthStatus]
}
