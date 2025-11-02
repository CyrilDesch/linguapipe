package com.cyrelis.srag.infrastructure.adapters.driven.database.postgres.models

import java.sql.Timestamp
import java.util.UUID

import com.cyrelis.srag.domain.transcript.{IngestSource, LanguageCode, Transcript}
import io.circe.parser.*
import io.circe.syntax.*
import io.getquill.JsonbValue

final case class TranscriptRow(
  id: UUID,
  language: Option[String],
  text: String,
  confidence: Double,
  createdAt: Timestamp,
  source: String,
  metadata: JsonbValue[String]
)

object TranscriptRow:
  def fromTranscript(transcript: Transcript): TranscriptRow =
    val metadatasJson = transcript.metadata.asJson.noSpaces
    val source        = sourceToString(transcript.source)
    val createdTs     = Timestamp.from(transcript.createdAt)

    TranscriptRow(
      id = transcript.id,
      language = transcript.language.map(_.value),
      text = transcript.text,
      confidence = transcript.confidence,
      createdAt = createdTs,
      source = source,
      metadata = JsonbValue(metadatasJson)
    )

  def toTranscript(row: TranscriptRow): Transcript =
    val metadata  = decode[Map[String, String]](row.metadata.value).getOrElse(Map.empty)
    val source    = stringToSource(row.source)
    val createdAt = row.createdAt.toInstant
    val language  = row.language.map(LanguageCode.unsafe)

    Transcript(
      id = row.id,
      language = language,
      text = row.text,
      confidence = row.confidence,
      createdAt = createdAt,
      source = source,
      metadata = metadata
    )

  private def sourceToString(source: IngestSource): String =
    source match
      case IngestSource.Audio    => "Audio"
      case IngestSource.Text     => "Text"
      case IngestSource.Document => "Document"

  private def stringToSource(source: String): IngestSource =
    source match
      case "Audio"    => IngestSource.Audio
      case "Text"     => IngestSource.Text
      case "Document" => IngestSource.Document
      case _          => IngestSource.Text
