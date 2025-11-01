package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.main

import com.cyrelis.srag.domain.transcript.Transcript
import com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.common.IngestSourceDto
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TranscriptRestDto(
  id: String,
  text: String,
  createdAt: String,
  source: IngestSourceDto,
  language: Option[String],
  metadata: Map[String, String]
)

object TranscriptRestDto {
  given Codec[TranscriptRestDto] = deriveCodec

  def fromDomain(transcript: Transcript): TranscriptRestDto =
    TranscriptRestDto(
      id = transcript.id.toString,
      text = transcript.text,
      createdAt = transcript.createdAt.toString,
      source = IngestSourceDto.fromDomain(transcript.source),
      language = transcript.language.map(_.value),
      metadata = transcript.metadata
    )
}
