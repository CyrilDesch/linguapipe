package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestResultRestDto(
  result: String
)

object TestResultRestDto {
  given Codec[TestResultRestDto] = deriveCodec
}
