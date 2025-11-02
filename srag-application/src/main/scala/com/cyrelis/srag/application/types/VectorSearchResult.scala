package com.cyrelis.srag.application.types

import java.util.UUID

final case class VectorSearchResult(
  transcriptId: UUID,
  segmentIndex: Int,
  score: Double
)
