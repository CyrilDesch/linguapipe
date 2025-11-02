package com.cyrelis.srag.application.ports.driving

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.{ContextSegment, VectorStoreFilter}
import zio.*

trait QueryPort {
  def retrieveContext(
    queryText: String,
    filter: Option[VectorStoreFilter],
    limit: Int
  ): ZIO[Any, PipelineError, List[ContextSegment]]
}
