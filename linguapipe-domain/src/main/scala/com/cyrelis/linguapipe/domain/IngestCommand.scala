package com.cyrelis.linguapipe.domain

import java.util.UUID

import zio.json.*

final case class IngestionResult(
  transcriptId: UUID,
  segmentsEmbedded: Int,
  completedAt: java.time.Instant
)

object IngestionResult {
  given JsonEncoder[IngestionResult] = DeriveJsonEncoder.gen[IngestionResult]
  given JsonDecoder[IngestionResult] = DeriveJsonDecoder.gen[IngestionResult]
}
