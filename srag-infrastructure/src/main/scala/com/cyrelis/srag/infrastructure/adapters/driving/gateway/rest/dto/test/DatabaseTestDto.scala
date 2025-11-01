package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test

import com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.common.IngestSourceDto
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestDatabaseRestDto(
  text: String,
  source: IngestSourceDto
)

object TestDatabaseRestDto {
  given Codec[TestDatabaseRestDto] = deriveCodec
}
