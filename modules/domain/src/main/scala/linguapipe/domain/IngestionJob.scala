package linguapipe.domain

import java.time.Instant

/** User-initiated ingestion pipeline run */
final case class IngestionJobId(value: String) extends AnyVal

enum IngestSource:
  case Audio, Text, Document

enum JobStatus:
  case Pending, Running, Completed, Failed

final case class IngestionJob(
  id: IngestionJobId,
  groupId: Option[String],
  source: IngestSource,
  status: JobStatus,
  requestedAt: Instant,
  completedAt: Option[Instant],
  metadata: TranscriptMetadata
)
