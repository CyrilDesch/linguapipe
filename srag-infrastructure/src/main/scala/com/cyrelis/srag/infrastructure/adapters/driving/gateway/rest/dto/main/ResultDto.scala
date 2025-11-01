package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.main

import com.cyrelis.srag.domain.ingestionjob.IngestionJob
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class JobAcceptedRestDto(
  jobId: String,
  status: String
)

object JobAcceptedRestDto {
  given Codec[JobAcceptedRestDto] = deriveCodec

  def fromDomain(job: IngestionJob): JobAcceptedRestDto =
    JobAcceptedRestDto(
      jobId = job.id.toString,
      status = job.status.toString
    )
}

final case class JobStatusRestDto(
  jobId: String,
  transcriptId: Option[String],
  status: String,
  attempt: Int,
  maxAttempts: Int,
  errorMessage: Option[String],
  createdAt: String,
  updatedAt: String
)

object JobStatusRestDto {
  given Codec[JobStatusRestDto] = deriveCodec

  def fromDomain(job: IngestionJob): JobStatusRestDto =
    JobStatusRestDto(
      jobId = job.id.toString,
      transcriptId = job.transcriptId.map(_.toString),
      status = job.status.toString,
      attempt = job.attempt,
      maxAttempts = job.maxAttempts,
      errorMessage = job.errorMessage,
      createdAt = job.createdAt.toString,
      updatedAt = job.updatedAt.toString
    )
}
