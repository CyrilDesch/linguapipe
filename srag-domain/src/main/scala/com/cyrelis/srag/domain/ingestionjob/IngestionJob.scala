package com.cyrelis.srag.domain.ingestionjob

import java.time.Instant
import java.util.UUID

import com.cyrelis.srag.domain.transcript.IngestSource

enum JobStatus:
  case Pending
  case Transcribing
  case Embedding
  case Indexing
  case Success
  case Failed
  case DeadLetter

final case class IngestionJob(
  id: UUID,
  transcriptId: Option[UUID],
  source: IngestSource,
  mediaContentType: Option[String],
  mediaFilename: Option[String],
  status: JobStatus,
  attempt: Int,
  maxAttempts: Int,
  errorMessage: Option[String],
  blobKey: Option[String],
  metadata: Map[String, String],
  createdAt: Instant,
  updatedAt: Instant
) {

  def markPending(): IngestionJob =
    copy(status = JobStatus.Pending, errorMessage = None, updatedAt = Instant.now())

  def markTranscribing(): IngestionJob =
    copy(status = JobStatus.Transcribing, updatedAt = Instant.now())

  def markEmbedding(): IngestionJob =
    copy(status = JobStatus.Embedding, updatedAt = Instant.now())

  def markIndexing(): IngestionJob =
    copy(status = JobStatus.Indexing, updatedAt = Instant.now())

  def markSuccess(): IngestionJob =
    copy(status = JobStatus.Success, errorMessage = None, updatedAt = Instant.now())

  def markFailed(message: String): IngestionJob =
    copy(
      status = JobStatus.Failed,
      errorMessage = Some(message),
      updatedAt = Instant.now()
    )

  def markDeadLetter(message: String): IngestionJob =
    copy(status = JobStatus.DeadLetter, errorMessage = Some(message), updatedAt = Instant.now())

  def incrementAttempt(): IngestionJob =
    copy(attempt = attempt + 1, updatedAt = Instant.now())
}

object IngestionJob:
  def newAudioJob(
    source: IngestSource,
    mediaContentType: Option[String],
    mediaFilename: Option[String],
    blobKey: Option[String],
    metadata: Map[String, String],
    maxAttempts: Int,
    createdAt: Instant
  ): IngestionJob =
    val jobId = UUID.randomUUID()

    IngestionJob(
      id = jobId,
      transcriptId = None,
      source = source,
      mediaContentType = mediaContentType,
      mediaFilename = mediaFilename,
      status = JobStatus.Pending,
      attempt = 0,
      maxAttempts = maxAttempts,
      errorMessage = None,
      blobKey = blobKey,
      metadata = metadata,
      createdAt = createdAt,
      updatedAt = createdAt
    )

  def newTextJob(
    blobKey: String,
    metadata: Map[String, String],
    maxAttempts: Int,
    createdAt: Instant
  ): IngestionJob =
    val jobId = UUID.randomUUID()

    IngestionJob(
      id = jobId,
      transcriptId = None,
      source = IngestSource.Text,
      mediaContentType = None,
      mediaFilename = None,
      status = JobStatus.Pending,
      attempt = 0,
      maxAttempts = maxAttempts,
      errorMessage = None,
      blobKey = Some(blobKey),
      metadata = metadata,
      createdAt = createdAt,
      updatedAt = createdAt
    )
