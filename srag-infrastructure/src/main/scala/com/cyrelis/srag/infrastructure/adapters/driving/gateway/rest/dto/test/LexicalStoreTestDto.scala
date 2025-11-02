package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestLexicalStoreIndexRestDto(
  segments: List[String],
  metadata: Map[String, String] = Map.empty
)

object TestLexicalStoreIndexRestDto {
  given Codec[TestLexicalStoreIndexRestDto] = deriveCodec
}

final case class TestLexicalStoreQueryRestDto(
  queryText: String,
  limit: Int
)

object TestLexicalStoreQueryRestDto {
  given Codec[TestLexicalStoreQueryRestDto] = deriveCodec
}

final case class LexicalSearchResultDto(
  text: String,
  score: Double
)

object LexicalSearchResultDto {
  given Codec[LexicalSearchResultDto] = deriveCodec
}

final case class TestLexicalStoreQueryResultRestDto(
  results: List[LexicalSearchResultDto],
  totalResults: Int
)

object TestLexicalStoreQueryResultRestDto {
  given Codec[TestLexicalStoreQueryResultRestDto] = deriveCodec
}
