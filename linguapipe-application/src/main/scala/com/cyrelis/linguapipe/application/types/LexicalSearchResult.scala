package com.cyrelis.linguapipe.application.types

import java.util.UUID

final case class LexicalSearchResult(
  transcriptId: UUID,
  segmentIndex: Int,
  score: Double,
  text: String,
  metadata: Map[String, String]
)
