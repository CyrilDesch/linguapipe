package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.main

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class QueryRequestDto(
  query: String,
  limit: Option[Int] = Some(5),
  metadata: Option[Map[String, String]] = None
)

object QueryRequestDto {
  given Codec[QueryRequestDto] = deriveCodec
}
