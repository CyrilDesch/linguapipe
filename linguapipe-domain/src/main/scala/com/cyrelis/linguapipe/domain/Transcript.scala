package com.cyrelis.linguapipe.domain

import java.time.Instant
import java.util.UUID

final case class Transcript(
  id: UUID,
  language: String,
  text: String,
  createdAt: Instant,
  metadata: TranscriptMetadata
)

final case class TranscriptMetadata(
  source: IngestSource,
  attributes: Map[String, String]
)
