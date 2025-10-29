package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import java.io.File

import sttp.model.Part
import zio.json.*

final case class TestTranscriberRestDto(file: Part[File])
final case class TestTranscriberResultRestDto(result: String)

final case class TestEmbedderRestDto(content: String)
final case class TestBlobStoreRestDto(content: String)
final case class TestDocumentParserRestDto(content: String)

final case class TestResultRestDto(
  result: String
)

object TestEmbedderRestDto {
  given JsonEncoder[TestEmbedderRestDto] = DeriveJsonEncoder.gen[TestEmbedderRestDto]
  given JsonDecoder[TestEmbedderRestDto] = DeriveJsonDecoder.gen[TestEmbedderRestDto]
}

object TestTranscriberResultRestDto {
  given JsonEncoder[TestTranscriberResultRestDto] = DeriveJsonEncoder.gen[TestTranscriberResultRestDto]
  given JsonDecoder[TestTranscriberResultRestDto] = DeriveJsonDecoder.gen[TestTranscriberResultRestDto]
}

object TestBlobStoreRestDto {
  given JsonEncoder[TestBlobStoreRestDto] = DeriveJsonEncoder.gen[TestBlobStoreRestDto]
  given JsonDecoder[TestBlobStoreRestDto] = DeriveJsonDecoder.gen[TestBlobStoreRestDto]
}

object TestDocumentParserRestDto {
  given JsonEncoder[TestDocumentParserRestDto] = DeriveJsonEncoder.gen[TestDocumentParserRestDto]
  given JsonDecoder[TestDocumentParserRestDto] = DeriveJsonDecoder.gen[TestDocumentParserRestDto]
}

object TestResultRestDto {
  given JsonEncoder[TestResultRestDto] = DeriveJsonEncoder.gen[TestResultRestDto]
  given JsonDecoder[TestResultRestDto] = DeriveJsonDecoder.gen[TestResultRestDto]
}
