package com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres.models

import java.sql.Timestamp
import java.util.UUID

import com.cyrelis.linguapipe.domain.ingestionjob.{IngestionJob, JobStatus}
import com.cyrelis.linguapipe.domain.transcript.IngestSource
import io.circe.parser.*
import io.circe.syntax.*
import io.getquill.JsonbValue

final case class IngestionJobRow(
  id: UUID,
  transcriptId: Option[UUID],
  source: String,
  mediaFormat: Option[String],
  mediaFilename: Option[String],
  status: String,
  attempt: Int,
  maxAttempts: Int,
  errorMessage: Option[String],
  blobKey: Option[String],
  metadata: JsonbValue[String],
  createdAt: Timestamp,
  updatedAt: Timestamp
)

object IngestionJobRow {
  def fromDomain(job: IngestionJob): IngestionJobRow =
    IngestionJobRow(
      id = job.id,
      transcriptId = job.transcriptId,
      source = sourceToString(job.source),
      mediaFormat = job.mediaFormat,
      mediaFilename = job.mediaFilename,
      status = job.status.toString,
      attempt = job.attempt,
      maxAttempts = job.maxAttempts,
      errorMessage = job.errorMessage,
      blobKey = job.blobKey,
      metadata = JsonbValue(job.metadata.asJson.noSpaces),
      createdAt = Timestamp.from(job.createdAt),
      updatedAt = Timestamp.from(job.updatedAt)
    )

  def toDomain(row: IngestionJobRow): IngestionJob =
    val metadata = decode[Map[String, String]](row.metadata.value).getOrElse(Map.empty)
    IngestionJob(
      id = row.id,
      transcriptId = row.transcriptId,
      source = stringToSource(row.source),
      mediaFormat = row.mediaFormat,
      mediaFilename = row.mediaFilename,
      status = JobStatus.valueOf(row.status),
      attempt = row.attempt,
      maxAttempts = row.maxAttempts,
      errorMessage = row.errorMessage,
      blobKey = row.blobKey,
      metadata = metadata,
      createdAt = row.createdAt.toInstant,
      updatedAt = row.updatedAt.toInstant
    )

  private def sourceToString(source: IngestSource): String =
    source match
      case IngestSource.Audio    => "Audio"
      case IngestSource.Text     => "Text"
      case IngestSource.Document => "Document"

  private def stringToSource(value: String): IngestSource =
    value match
      case "Audio"    => IngestSource.Audio
      case "Text"     => IngestSource.Text
      case "Document" => IngestSource.Document
      case other      => throw new IllegalArgumentException(s"Unknown ingest source: $other")
}
