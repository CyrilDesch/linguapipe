package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import zio.json.*

final case class AudioIngestRestDto(content: String, format: String, language: Option[String])
final case class TextIngestRestDto(content: String, language: Option[String])
final case class DocumentIngestRestDto(content: String, mediaType: String, language: Option[String])

final case class IngestionResultRestDto(
  transcriptId: String,
  segmentsEmbedded: Int,
  completedAt: String
)

object AudioIngestRestDto {
  given JsonEncoder[AudioIngestRestDto] = DeriveJsonEncoder.gen[AudioIngestRestDto]
  given JsonDecoder[AudioIngestRestDto] = DeriveJsonDecoder.gen[AudioIngestRestDto]
}

object TextIngestRestDto {
  given JsonEncoder[TextIngestRestDto] = DeriveJsonEncoder.gen[TextIngestRestDto]
  given JsonDecoder[TextIngestRestDto] = DeriveJsonDecoder.gen[TextIngestRestDto]
}

object DocumentIngestRestDto {
  given JsonEncoder[DocumentIngestRestDto] = DeriveJsonEncoder.gen[DocumentIngestRestDto]
  given JsonDecoder[DocumentIngestRestDto] = DeriveJsonDecoder.gen[DocumentIngestRestDto]
}

object IngestionResultRestDto {
  given JsonEncoder[IngestionResultRestDto] = DeriveJsonEncoder.gen[IngestionResultRestDto]
  given JsonDecoder[IngestionResultRestDto] = DeriveJsonDecoder.gen[IngestionResultRestDto]
}
