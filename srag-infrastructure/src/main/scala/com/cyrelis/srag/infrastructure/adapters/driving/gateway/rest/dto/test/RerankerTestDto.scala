package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestRerankerRestDto(
  query: String,
  candidates: List[String],
  topK: Int
)

object TestRerankerRestDto {
  given Codec[TestRerankerRestDto] = deriveCodec
}

final case class RerankerResultDto(
  text: String,
  score: Double
)

object RerankerResultDto {
  given Codec[RerankerResultDto] = deriveCodec
}

final case class TestRerankerResultRestDto(
  results: List[RerankerResultDto]
)

object TestRerankerResultRestDto {
  given Codec[TestRerankerResultRestDto] = deriveCodec
}
