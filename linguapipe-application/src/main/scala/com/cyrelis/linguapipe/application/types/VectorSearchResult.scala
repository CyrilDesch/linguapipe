package com.cyrelis.linguapipe.application.types

import java.util.UUID

final case class VectorSearchResult(
  transcriptId: UUID,
  segmentIndex: Int,
  score: Double,
  metadata: Option[Map[String, String]] = None // Temporairement: pour retourner les métadonnées du payload
)
