package com.cyrelis.linguapipe.domain.transcript

import java.time.Instant
import java.util.UUID

import com.cyrelis.linguapipe.domain.transcript.{IngestSource, LanguageCode}

final case class Transcript(
  id: UUID,
  language: Option[LanguageCode],
  text: String,
  confidence: Double,
  createdAt: Instant,
  source: IngestSource,
  metadata: Map[String, String]
):
  def addMetadatas(extra: Map[String, String]): Transcript =
    copy(metadata = metadata ++ extra)

  def addMetadata(key: String, value: String): Transcript =
    copy(metadata = metadata + (key -> value))
