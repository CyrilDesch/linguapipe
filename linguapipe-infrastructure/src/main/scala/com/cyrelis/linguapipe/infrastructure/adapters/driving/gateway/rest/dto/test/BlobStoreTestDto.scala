package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestBlobStoreRestDto(content: String)

object TestBlobStoreRestDto {
  given Codec[TestBlobStoreRestDto] = deriveCodec
}
