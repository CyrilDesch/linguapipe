package com.cyrelis.srag.application.types

import java.util.UUID

final case class RerankerCandidate(
  transcriptId: UUID,
  segmentIndex: Int,
  text: String
)
