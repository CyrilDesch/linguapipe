package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.common

import com.cyrelis.srag.domain.transcript.IngestSource
import io.circe.Codec
import sttp.tapir.Schema

enum IngestSourceDto:
  case Audio, Text, Document

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

  def toDomain(source: IngestSourceDto): IngestSource =
    source match
      case IngestSourceDto.Audio    => IngestSource.Audio
      case IngestSourceDto.Text     => IngestSource.Text
      case IngestSourceDto.Document => IngestSource.Document

  def fromDomain(source: IngestSource): IngestSourceDto =
    source match
      case IngestSource.Audio    => IngestSourceDto.Audio
      case IngestSource.Text     => IngestSourceDto.Text
      case IngestSource.Document => IngestSourceDto.Document
}
