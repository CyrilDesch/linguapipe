package com.cyrelis.srag.application.ports.driven.storage

import java.util.UUID

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.{HealthStatus, VectorSearchResult, VectorStoreFilter}
import zio.*

trait VectorStorePort {
  def upsertEmbeddings(
    transcriptId: UUID,
    vectors: List[Array[Float]],
    metadata: Map[String, String]
  ): ZIO[Any, PipelineError, Unit]
  def searchSimilar(
    queryVector: Array[Float],
    limit: Int,
    filter: Option[VectorStoreFilter] = None
  ): ZIO[Any, PipelineError, List[VectorSearchResult]]
  def healthCheck(): Task[HealthStatus]
}
