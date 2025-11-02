package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.testui

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class AdminJobDto(
  jobId: String,
  transcriptId: Option[String],
  status: String,
  attempt: Int,
  maxAttempts: Int,
  errorMessage: Option[String],
  source: Option[String],
  createdAt: String,
  updatedAt: String,
  metadata: Option[Map[String, String]]
)

object AdminJobDto {
  given Codec[AdminJobDto] = deriveCodec
}

final case class AdminBlobDto(
  key: String,
  filename: Option[String],
  contentType: Option[String],
  size: Option[Long],
  created: Option[String]
)

object AdminBlobDto {
  given Codec[AdminBlobDto] = deriveCodec
}

final case class AdminVectorDto(
  id: String,
  transcriptId: Option[String],
  segmentIndex: Option[Int],
  vector: Option[List[Float]],
  payload: Option[Map[String, String]]
)

object AdminVectorDto {
  given Codec[AdminVectorDto] = deriveCodec
}

final case class AdminVectorsResponse(
  total: Int,
  vectors: List[AdminVectorDto]
)

object AdminVectorsResponse {
  given Codec[AdminVectorsResponse] = deriveCodec
}

final case class AdminOpenSearchDocument(
  id: String,
  transcriptId: Option[String],
  segmentIndex: Option[Int],
  text: Option[String],
  metadata: Option[Map[String, String]]
)

object AdminOpenSearchDocument {
  given Codec[AdminOpenSearchDocument] = deriveCodec
}

final case class AdminOpenSearchResponse(
  total: Int,
  documents: List[AdminOpenSearchDocument]
)

object AdminOpenSearchResponse {
  given Codec[AdminOpenSearchResponse] = deriveCodec
}
