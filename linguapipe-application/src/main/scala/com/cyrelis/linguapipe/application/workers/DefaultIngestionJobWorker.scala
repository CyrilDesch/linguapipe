package com.cyrelis.linguapipe.application.workers

import java.time.Instant

import scala.math.pow

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.job.JobQueuePort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, LexicalStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.application.types.JobProcessingConfig
import com.cyrelis.linguapipe.domain.ingestionjob.{IngestionJob, IngestionJobRepository, JobStatus}
import com.cyrelis.linguapipe.domain.transcript.{IngestSource, TranscriptRepository}
import zio.{Duration, *}

final class DefaultIngestionJobWorker(
  jobRepository: IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]],
  blobStore: BlobStorePort,
  transcriber: TranscriberPort,
  embedder: EmbedderPort,
  transcriptRepository: TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]],
  vectorSink: VectorStorePort,
  lexicalStore: LexicalStorePort,
  jobConfig: JobProcessingConfig,
  jobQueue: JobQueuePort
) {

  def run: ZIO[Any, Nothing, Unit] = (for {
    jobsIds <- jobQueue
                 .dequeueBatch(jobConfig.batchSize)
                 .catchAll(err => ZIO.logWarning(s"dequeue failed: ${err.message}").as(Nil))
    _ <- if (jobsIds.isEmpty) ZIO.sleep(Duration.fromScala(jobConfig.pollInterval)) else ZIO.unit
    _ <- ZIO.foreachParDiscard(jobsIds)(jid =>
           processById(jid).foldZIO(
             err => handleFailureById(jid, err),
             _ => jobQueue.ack(jid).catchAll(e => ZIO.logWarning(s"ack failed for $jid: ${e.message}"))
           )
         )
  } yield ()).forever

  private def processById(jobId: java.util.UUID): ZIO[Any, PipelineError, Unit] =
    for {
      jobOpt <- jobRepository.findById(jobId)
      job    <- ZIO.fromOption(jobOpt).orElseFail(PipelineError.DatabaseError(s"Job $jobId not found", None))
      _      <- processSingleJob(job)
    } yield ()

  private def processSingleJob(job: IngestionJob): ZIO[Any, PipelineError, Unit] =
    for {
      claimedJob <- ZIO.succeed(
                      job
                        .incrementAttempt()
                        .markTranscribing()
                    )
      jobState1 <- jobRepository.update(claimedJob)
      blobKey   <- ZIO
                   .fromOption(jobState1.blobKey)
                   .orElseFail(PipelineError.DatabaseError(s"Missing blob key for job ${jobState1.id}", None))
      contentType <-
        ZIO
          .fromOption(jobState1.mediaContentType)
          .orElseFail(PipelineError.DatabaseError(s"Missing media content type for job ${jobState1.id}", None))
      mediaFilename <-
        ZIO
          .fromOption(jobState1.mediaFilename)
          .orElseFail(PipelineError.DatabaseError(s"Missing media filename for job ${jobState1.id}", None))
      audio           <- blobStore.fetchAudio(blobKey)
      temp_transcript <- transcriber
                           .transcribe(audio, contentType, mediaFilename)
      transcript  = temp_transcript.addMetadatas(jobState1.metadata)
      _          <- transcriptRepository.persist(transcript)
      jobWithTid <- jobRepository.update(
                      jobState1.copy(transcriptId = Some(transcript.id))
                    )
      jobState2 <-
        jobRepository.update(
          jobWithTid.markEmbedding()
        )
      segments  <- embedder.embed(transcript)
      jobState3 <-
        jobRepository.update(
          jobState2.markIndexing()
        )
      chunkVectors        = segments.map(_._2)
      chunkTextsWithIndex = segments.zipWithIndex.map { case ((text, _), index) => (index, text) }
      _                   <- vectorSink.upsertEmbeddings(transcript.id, chunkVectors, transcript.metadata)
      _ <- lexicalStore
             .deleteTranscript(transcript.id)
             .catchAll(error => ZIO.logWarning(s"Failed to purge lexical index for ${transcript.id}: ${error.message}"))
      _ <- lexicalStore.indexSegments(transcript.id, chunkTextsWithIndex, transcript.metadata)
      finalState <-
        jobRepository.update(
          jobState3.markSuccess()
        )
      _ <- blobStore
             .deleteBlob(blobKey)
             .catchAll(error => ZIO.logWarning(s"Failed to delete blob for job ${finalState.id}: ${error.message}"))
    } yield ()

  private def handleFailure(job: IngestionJob, error: PipelineError): ZIO[Any, Nothing, Unit] =
    for {
      now       <- Clock.instant
      latestJob <-
        jobRepository
          .findById(job.id)
          .catchAll(err =>
            ZIO.logError(s"Failed to load job ${job.id} during failure handling: ${err.message}").as(Some(job))
          )
          .map(_.getOrElse(job))
      _ <-
        if (latestJob.status == JobStatus.Success) ZIO.unit
        else {
          val retryAt    = computeNextRetry(latestJob, now)
          val updatedJob =
            if (retryAt.isDefined) latestJob.markFailed(error.message) else latestJob.markDeadLetter(error.message)
          for {
            updateOk <- jobRepository
                          .update(updatedJob)
                          .as(true)
                          .catchAll(err =>
                            ZIO.logError(s"Failed to update job ${updatedJob.id} after error: ${err.message}").as(false)
                          )
            _ <-
              ZIO.logError(
                s"Ingestion job ${job.id} failed with '${error.message}'. Next retry at ${retryAt.map(_.toString).getOrElse("n/a")} (attempt ${updatedJob.attempt}/${updatedJob.maxAttempts})"
              )
            _ <- if (updateOk) {
                   retryAt match
                     case Some(next) =>
                       jobQueue
                         .retry(
                           job.id,
                           updatedJob.attempt,
                           zio.Duration.fromMillis(java.time.Duration.between(now, next).toMillis)
                         )
                         .catchAll(e => ZIO.logWarning(s"retry enqueue failed for ${job.id}: ${e.message}"))
                     case None =>
                       jobQueue
                         .deadLetter(job.id, error.message)
                         .catchAll(e => ZIO.logWarning(s"dead-letter failed for ${job.id}: ${e.message}"))
                 } else {
                   ZIO.logWarning(
                     s"Skipping re-enqueue for ${job.id} because state persistence failed; manual intervention may be required"
                   )
                 }
          } yield ()
        }
    } yield ()

  private def handleFailureById(jobId: java.util.UUID, error: PipelineError): ZIO[Any, Nothing, Unit] =
    for {
      now    <- Clock.instant
      jobOpt <- jobRepository.findById(jobId).orElseSucceed(None)
      baseJob = jobOpt.getOrElse(
                  IngestionJob(
                    id = jobId,
                    transcriptId = None,
                    source = IngestSource.Audio,
                    mediaContentType = None,
                    mediaFilename = None,
                    status = JobStatus.Failed,
                    attempt = 0,
                    maxAttempts = 1,
                    errorMessage = Some(error.message),
                    blobKey = None,
                    metadata = Map.empty,
                    createdAt = now,
                    updatedAt = now
                  )
                )
      _ <- handleFailure(baseJob, error)
    } yield ()

  private def computeNextRetry(job: IngestionJob, now: Instant): Option[Instant] =
    if (job.attempt >= job.maxAttempts) None
    else {
      val rawDelayMs = jobConfig.initialRetryDelay.toMillis * pow(jobConfig.backoffFactor, job.attempt.toDouble - 1.0)
      val cappedMs   = math.min(rawDelayMs, jobConfig.maxRetryDelay.toMillis.toDouble).toLong
      Some(now.plusMillis(cappedMs))
    }
}
