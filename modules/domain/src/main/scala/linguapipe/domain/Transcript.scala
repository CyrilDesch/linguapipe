package linguapipe.domain

import java.time.Instant

/** Core transcript model */
final case class TranscriptId(value: String) extends AnyVal

final case class SegmentId(value: String) extends AnyVal

final case class Segment(
  id: SegmentId,
  transcriptId: TranscriptId,
  text: String,
  startAt: Option[Instant],
  endAt: Option[Instant],
  metadata: SegmentMetadata
)

final case class Transcript(
  id: TranscriptId,
  language: String,
  segments: List[Segment],
  createdAt: Instant,
  metadata: TranscriptMetadata
)

/** Segment metadata */
final case class SegmentMetadata(
  speaker: Option[String],
  confidence: Option[Double],
  tags: Set[String]
)

/** Transcript metadata */
final case class TranscriptMetadata(
  source: IngestSource,
  attributes: Map[String, String]
)
