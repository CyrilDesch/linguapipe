package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test

import java.io.File

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.model.Part

final case class TestTranscriberRestDto(file: Part[File])

final case class TestTranscriberResultRestDto(result: String)

object TestTranscriberResultRestDto {
  given Codec[TestTranscriberResultRestDto] = deriveCodec
}
