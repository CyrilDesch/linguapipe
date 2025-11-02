package com.cyrelis.srag.application.types

import java.util.UUID

final case class ContextSegment(
  transcriptId: UUID,
  segmentIndex: Int,
  text: String,
  score: Double
)
