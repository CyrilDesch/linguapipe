package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestDocumentParserRestDto(content: String)

object TestDocumentParserRestDto {
  given Codec[TestDocumentParserRestDto] = deriveCodec
}
