package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import java.io.File

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.model.Part
import sttp.tapir.Schema

enum IngestSourceDto:
  case Audio, Text, Document

final case class TestTranscriberRestDto(file: Part[File])
final case class TestTranscriberResultRestDto(result: String)

final case class TestEmbedderRestDto(content: String)
final case class TestBlobStoreRestDto(content: String)
final case class TestDocumentParserRestDto(content: String)

final case class TestDatabaseRestDto(
  text: String,
  source: IngestSourceDto
)

final case class TestVectorStoreRestDto(
  transcriptId: String,
  vectors: List[List[Float]]
)

final case class TestResultRestDto(
  result: String
)

final case class TranscriptResponseDto(
  id: String,
  text: String,
  createdAt: String,
  source: IngestSourceDto,
  language: Option[String],
  attributes: Map[String, String]
)

object IngestSourceDto {
  given Codec[IngestSourceDto] = Codec.from(
    io.circe.Decoder.decodeString.emap {
      case "Audio"    => Right(IngestSourceDto.Audio)
      case "Text"     => Right(IngestSourceDto.Text)
      case "Document" => Right(IngestSourceDto.Document)
      case other      => Left(s"Unknown IngestSourceDto: $other")
    },
    io.circe.Encoder.encodeString.contramap(_.toString)
  )

  given Schema[IngestSourceDto] = Schema.derivedEnumeration[IngestSourceDto].defaultStringBased

  def toDomain(source: IngestSourceDto): com.cyrelis.linguapipe.domain.IngestSource =
    source match
      case IngestSourceDto.Audio    => com.cyrelis.linguapipe.domain.IngestSource.Audio
      case IngestSourceDto.Text     => com.cyrelis.linguapipe.domain.IngestSource.Text
      case IngestSourceDto.Document => com.cyrelis.linguapipe.domain.IngestSource.Document

  def fromDomain(source: com.cyrelis.linguapipe.domain.IngestSource): IngestSourceDto =
    source match
      case com.cyrelis.linguapipe.domain.IngestSource.Audio    => IngestSourceDto.Audio
      case com.cyrelis.linguapipe.domain.IngestSource.Text     => IngestSourceDto.Text
      case com.cyrelis.linguapipe.domain.IngestSource.Document => IngestSourceDto.Document
}

object TestEmbedderRestDto {
  given Codec[TestEmbedderRestDto] = deriveCodec
}

object TestTranscriberResultRestDto {
  given Codec[TestTranscriberResultRestDto] = deriveCodec
}

object TestBlobStoreRestDto {
  given Codec[TestBlobStoreRestDto] = deriveCodec
}

object TestDocumentParserRestDto {
  given Codec[TestDocumentParserRestDto] = deriveCodec
}

object TestDatabaseRestDto {
  given Codec[TestDatabaseRestDto] = deriveCodec
}

object TestVectorStoreRestDto {
  given Codec[TestVectorStoreRestDto] = deriveCodec
}

object TestResultRestDto {
  given Codec[TestResultRestDto] = deriveCodec
}

object TranscriptResponseDto {
  given Codec[TranscriptResponseDto] = deriveCodec

  def fromDomain(transcript: com.cyrelis.linguapipe.domain.Transcript): TranscriptResponseDto =
    TranscriptResponseDto(
      id = transcript.id.toString,
      text = transcript.text,
      createdAt = transcript.createdAt.toString,
      source = IngestSourceDto.fromDomain(transcript.source),
      language = transcript.language.map(_.value),
      attributes = transcript.attributes
    )
}
