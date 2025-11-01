package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.main

import com.cyrelis.srag.application.types.ContextSegment
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class QueryResponseDto(
  transcriptId: String,
  segmentIndex: Int,
  text: String,
  score: Double
)

object QueryResponseDto {
  given Codec[QueryResponseDto] = deriveCodec

  def fromDomain(segment: ContextSegment): QueryResponseDto =
    QueryResponseDto(
      transcriptId = segment.transcriptId.toString,
      segmentIndex = segment.segmentIndex,
      text = segment.text,
      score = segment.score
    )
}
