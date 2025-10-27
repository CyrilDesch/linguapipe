package linguapipe.domain

import java.time.Instant
import java.util.UUID

import zio.json.*

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

object Transcript {
  given JsonEncoder[Transcript] = DeriveJsonEncoder.gen[Transcript]
  given JsonDecoder[Transcript] = DeriveJsonDecoder.gen[Transcript]

  def fromText(
    content: String,
    language: Option[String],
    baseMetadata: TranscriptMetadata
  ): Transcript = {
    val transcriptId = UUID.randomUUID()
    val now          = Instant.now()

    Transcript(
      id = transcriptId,
      language = language.getOrElse("unknown"),
      text = content,
      createdAt = now,
      metadata = baseMetadata.copy(
        source = IngestSource.Text,
        attributes = baseMetadata.attributes + ("processing_method" -> "direct_text")
      )
    )
  }
}

object TranscriptMetadata {
  given JsonEncoder[TranscriptMetadata] = DeriveJsonEncoder.gen[TranscriptMetadata]
  given JsonDecoder[TranscriptMetadata] = DeriveJsonDecoder.gen[TranscriptMetadata]
}
