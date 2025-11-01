package com.cyrelis.srag.application.ports.driven.storage

import java.util.UUID

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.{HealthStatus, LexicalSearchResult, VectorStoreFilter}
import zio.*

trait LexicalStorePort {
  def indexSegments(
    transcriptId: UUID,
    segments: List[(Int, String)],
    metadata: Map[String, String]
  ): ZIO[Any, PipelineError, Unit]

  def deleteTranscript(transcriptId: UUID): ZIO[Any, PipelineError, Unit]

  def search(
    queryText: String,
    limit: Int,
    filter: Option[VectorStoreFilter] = None
  ): ZIO[Any, PipelineError, List[LexicalSearchResult]]

  def healthCheck(): Task[HealthStatus]
}
