package com.cyrelis.linguapipe.domain

import java.time.Instant
import java.util.UUID

import zio.json.*

enum IngestSource:
  case Audio, Text, Document

enum JobStatus:
  case Pending, Running, Completed, Failed

final case class IngestionJob(
  id: UUID,
  groupId: Option[String],
  source: IngestSource,
  status: JobStatus,
  requestedAt: Instant,
  completedAt: Option[Instant],
  metadata: TranscriptMetadata
)

object IngestSource {
  given JsonEncoder[IngestSource] = DeriveJsonEncoder.gen[IngestSource]
  given JsonDecoder[IngestSource] = DeriveJsonDecoder.gen[IngestSource]
}

object JobStatus {
  given JsonEncoder[JobStatus] = DeriveJsonEncoder.gen[JobStatus]
  given JsonDecoder[JobStatus] = DeriveJsonDecoder.gen[JobStatus]
}

object IngestionJob {
  given JsonEncoder[IngestionJob] = DeriveJsonEncoder.gen[IngestionJob]
  given JsonDecoder[IngestionJob] = DeriveJsonDecoder.gen[IngestionJob]
}
