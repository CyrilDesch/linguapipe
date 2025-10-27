package linguapipe.domain

import java.time.Instant
import java.util.UUID

import zio.json.*

final case class Segment(
  id: UUID,
  transcriptId: UUID,
  text: String,
  startAt: Option[Instant],
  endAt: Option[Instant],
  metadata: SegmentMetadata
)

final case class Transcript(
  id: UUID,
  language: String,
  segments: List[Segment],
  createdAt: Instant,
  metadata: TranscriptMetadata
)

final case class SegmentMetadata(
  speaker: Option[String],
  confidence: Option[Double],
  tags: Set[String]
)

final case class TranscriptMetadata(
  source: IngestSource,
  attributes: Map[String, String]
)

object Segment {
  given JsonEncoder[Segment] = DeriveJsonEncoder.gen[Segment]
  given JsonDecoder[Segment] = DeriveJsonDecoder.gen[Segment]
}

object Transcript {
  given JsonEncoder[Transcript] = DeriveJsonEncoder.gen[Transcript]
  given JsonDecoder[Transcript] = DeriveJsonDecoder.gen[Transcript]
}

object SegmentMetadata {
  given JsonEncoder[SegmentMetadata] = DeriveJsonEncoder.gen[SegmentMetadata]
  given JsonDecoder[SegmentMetadata] = DeriveJsonDecoder.gen[SegmentMetadata]
}

object TranscriptMetadata {
  given JsonEncoder[TranscriptMetadata] = DeriveJsonEncoder.gen[TranscriptMetadata]
  given JsonDecoder[TranscriptMetadata] = DeriveJsonDecoder.gen[TranscriptMetadata]
}
