package com.cyrelis.linguapipe.domain

import java.time.Instant
import java.util.UUID

final case class Transcript(
  id: UUID,
  language: Option[LanguageCode],
  text: String,
  confidence: Double,
  createdAt: Instant,
  source: IngestSource,
  attributes: Map[String, String]
)
