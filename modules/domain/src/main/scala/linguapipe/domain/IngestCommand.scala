package linguapipe.domain

final case class IngestCommand(
  jobId: IngestionJobId,
  payload: IngestPayload,
  metadata: TranscriptMetadata
)

enum IngestPayload:
  case InlineText(content: String, language: Option[String])
  case Base64Audio(content: String, format: String, language: Option[String])
  case Base64Document(content: String, mediaType: String, language: Option[String])

final case class IngestionResult(
  transcriptId: TranscriptId,
  segmentsEmbedded: Int,
  completedAt: java.time.Instant
)
