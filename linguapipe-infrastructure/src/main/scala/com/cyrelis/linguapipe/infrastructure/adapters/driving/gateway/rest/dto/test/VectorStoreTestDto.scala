package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestVectorStoreRestDto(
  text: String
)

object TestVectorStoreRestDto {
  given Codec[TestVectorStoreRestDto] = deriveCodec
}

final case class TestVectorStoreQueryRestDto(
  text: String
)

object TestVectorStoreQueryRestDto {
  given Codec[TestVectorStoreQueryRestDto] = deriveCodec
}

final case class VectorSearchResultDto(
  transcriptId: String,
  segmentIndex: Int,
  score: Double,
  transcriptText: Option[String] = None
)

object VectorSearchResultDto {
  given Codec[VectorSearchResultDto] = deriveCodec
}

final case class TestVectorStoreQueryResultRestDto(
  results: List[VectorSearchResultDto],
  totalResults: Int
)

object TestVectorStoreQueryResultRestDto {
  given Codec[TestVectorStoreQueryResultRestDto] = deriveCodec
}
