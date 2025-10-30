package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestVectorStoreRestDto(
  transcriptId: String,
  vectors: List[List[Float]]
)

object TestVectorStoreRestDto {
  given Codec[TestVectorStoreRestDto] = deriveCodec
}
