package com.cyrelis.srag.application.ports.driven.reranker

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.{HealthStatus, RerankerCandidate, RerankerResult}
import zio.*

trait RerankerPort {
  def rerank(
    query: String,
    candidates: List[RerankerCandidate],
    topK: Int
  ): ZIO[Any, PipelineError, List[RerankerResult]]

  def healthCheck(): Task[HealthStatus]
}
