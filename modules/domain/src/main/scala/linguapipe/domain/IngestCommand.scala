package linguapipe.domain

import java.util.UUID

import zio.json.*

final case class IngestCommand(
  jobId: UUID,
  payload: IngestPayload,
  metadata: TranscriptMetadata
)

enum IngestPayload:
  case InlineText(content: String, language: Option[String])
  case Base64Audio(content: String, format: String, language: Option[String])
  case Base64Document(content: String, mediaType: String, language: Option[String])

final case class IngestionResult(
  transcriptId: UUID,
  segmentsEmbedded: Int,
  completedAt: java.time.Instant
)

object IngestCommand {
  given JsonEncoder[IngestCommand] = DeriveJsonEncoder.gen[IngestCommand]
  given JsonDecoder[IngestCommand] = DeriveJsonDecoder.gen[IngestCommand]
}

object IngestPayload {
  given JsonEncoder[IngestPayload] = DeriveJsonEncoder.gen[IngestPayload]
  given JsonDecoder[IngestPayload] = DeriveJsonDecoder.gen[IngestPayload]
}

object IngestionResult {
  given JsonEncoder[IngestionResult] = DeriveJsonEncoder.gen[IngestionResult]
  given JsonDecoder[IngestionResult] = DeriveJsonDecoder.gen[IngestionResult]
}
