package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.test

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class TestEmbedderRestDto(content: String)

final case class EmbeddingChunkDto(
  chunk: String,
  dimensions: Int,
  embedding: List[Float]
)

final case class TestEmbedderResultRestDto(
  totalChunks: Int,
  chunks: List[EmbeddingChunkDto]
)

object TestEmbedderRestDto {
  given Codec[TestEmbedderRestDto] = deriveCodec
}

object EmbeddingChunkDto {
  given Codec[EmbeddingChunkDto] = deriveCodec
}

object TestEmbedderResultRestDto {
  given Codec[TestEmbedderResultRestDto] = deriveCodec
}
