package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.handler

import java.nio.file.Files
import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.RestUtils
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.main.*
import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.error.ErrorHandler
import zio.*

object MainHandlers {

  def handleHealth: ZIO[HealthCheckPort, String, List[HealthStatusRestDto]] =
    ZIO
      .serviceWithZIO[HealthCheckPort](_.checkAllServices())
      .map(_.map(HealthStatusRestDto.fromDomain))
      .mapError(_.getMessage)

  def handleIngestAudio(req: AudioIngestMultipartDto): ZIO[IngestPort, String, JobAcceptedRestDto] =
    (for {
      format     <- ZIO.succeed(RestUtils.extractFormat(req.file))
      audioBytes <- ZIO.attempt(Files.readAllBytes(req.file.body.toPath))
      job        <-
        ZIO.serviceWithZIO[IngestPort](
          _.submitAudio(audioBytes, format, req.file.fileName.getOrElse(UUID.randomUUID().toString), req.metadata)
        )
    } yield JobAcceptedRestDto.fromDomain(job)).mapError(ErrorHandler.errorToString)

  def handleIngestText(req: TextIngestRestDto): ZIO[IngestPort, String, JobAcceptedRestDto] =
    ZIO
      .serviceWithZIO[IngestPort](_.submitText(req.content, Map.empty))
      .map(JobAcceptedRestDto.fromDomain)
      .mapError(ErrorHandler.errorToString)

  def handleIngestDocument(req: DocumentIngestRestDto): ZIO[IngestPort, String, JobAcceptedRestDto] =
    ZIO
      .serviceWithZIO[IngestPort](_.submitDocument(req.content, req.mediaType, Map.empty))
      .map(JobAcceptedRestDto.fromDomain)
      .mapError(ErrorHandler.errorToString)

  def handleGetJobStatus(jobId: String): ZIO[IngestPort, String, JobStatusRestDto] =
    (for {
      parsedId <-
        ZIO
          .attempt(UUID.fromString(jobId))
          .mapError(_ => com.cyrelis.linguapipe.application.errors.PipelineError.ConfigurationError("Invalid job id"))
      jobOpt <- ZIO.serviceWithZIO[IngestPort](_.getJob(parsedId))
      job    <- ZIO
               .fromOption(jobOpt)
               .orElseFail(
                 com.cyrelis.linguapipe.application.errors.PipelineError.ConfigurationError(s"Job $jobId not found")
               )
    } yield JobStatusRestDto.fromDomain(job)).mapError(ErrorHandler.errorToString)

  def handleGetTranscripts(
    metadataFilters: Map[String, String],
    sortBy: Option[String],
    metadataSortKey: Option[String],
    order: Option[String]
  ): ZIO[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]], String, List[TranscriptRestDto]] =
    ZIO
      .serviceWithZIO[TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]](_.getAll())
      .map { transcripts =>
        val filtered = transcripts.filter { t =>
          val meta = t.metadata
          metadataFilters.forall { case (k, v) => meta.get(k).contains(v) }
        }

        val ascending = order.forall(_.equalsIgnoreCase("asc"))

        val sorted = (sortBy.map(_.toLowerCase) match {
          case Some("metadata") =>
            val key = metadataSortKey.getOrElse("")
            if (key.isEmpty) filtered.sortBy(_.createdAt.toString)
            else filtered.sortBy(t => t.metadata.getOrElse(key, ""))
          case _ =>
            filtered.sortBy(_.createdAt)
        })

        val maybeReversed = if (ascending) sorted else sorted.reverse
        maybeReversed.map(TranscriptRestDto.fromDomain)
      }
      .mapError(ErrorHandler.errorToString)
}
