package com.cyrelis.srag.application.workers

import java.time.Instant

import scala.math.pow

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.srag.application.ports.driven.job.JobQueuePort
import com.cyrelis.srag.application.ports.driven.storage.{BlobStorePort, LexicalStorePort, VectorStorePort}
import com.cyrelis.srag.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.srag.application.types.JobProcessingConfig
import com.cyrelis.srag.domain.ingestionjob.{IngestionJob, IngestionJobRepository, JobStatus}
import com.cyrelis.srag.domain.transcript.{IngestSource, TranscriptRepository}
import zio.*

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
    jobOpt <- jobQueue
                .claim(blockingTimeoutSec = jobConfig.pollInterval.toSeconds.toInt)
                .catchAll(err => ZIO.logWarning(s"claim failed: ${err.message}").as(None))
    _ <- jobOpt match {
           case Some(jobId) =>
             ZIO.scoped {
               ZIO
                 .acquireRelease(
                   (for {
                     _ <- ZIO.sleep((JobQueuePort.LockExpirationSeconds / 2).seconds)
                     _ <- jobQueue
                            .heartbeat(jobId)
                            .catchAll(e => ZIO.logWarning(s"Heartbeat failed for $jobId: ${e.message}"))
                   } yield ()).forever.forkDaemon
                 )(fiber => fiber.interrupt)
                 .flatMap { _ =>
                   ZIO.logDebug(s"Claimed job: $jobId") *>
                     processById(jobId).foldZIO(
                       err => handleFailureById(jobId, err),
                       _ =>
                         for {
                           _ <- ZIO.logDebug(s"Job $jobId processed successfully, acknowledging")
                           _ <-
                             jobQueue.ack(jobId).catchAll(e => ZIO.logWarning(s"ack failed for $jobId: ${e.message}"))
                         } yield ()
                     )
                 }
             }
           case None =>
             ZIO.unit
         }
  } yield ()).forever

  private def processById(jobId: java.util.UUID): ZIO[Any, PipelineError, Unit] =
    for {
      _      <- ZIO.logDebug(s"Starting processing for job $jobId")
      jobOpt <- jobRepository.findById(jobId)
      _      <- ZIO.logDebug(s"Job $jobId lookup: ${if (jobOpt.isDefined) "found" else "not found"}")
      job    <- ZIO.fromOption(jobOpt).orElseFail(PipelineError.DatabaseError(s"Job $jobId not found", None))
      _      <- ZIO.logDebug(s"Job $jobId status: ${job.status}, attempt: ${job.attempt}/${job.maxAttempts}")
      _      <- processSingleJob(job)
      _      <- ZIO.logDebug(s"Completed processing for job $jobId")
    } yield ()

  private def processSingleJob(job: IngestionJob): ZIO[Any, PipelineError, Unit] =
    for {
      _          <- ZIO.logDebug(s"Processing job ${job.id} - step 1: marking as transcribing (attempt ${job.attempt + 1})")
      claimedJob <- ZIO.succeed(
                      job
                        .incrementAttempt()
                        .markTranscribing()
                    )
      jobState1 <- jobRepository.update(claimedJob)
      _         <- ZIO.logDebug(s"Job ${jobState1.id} updated to status: ${jobState1.status}")
      blobKey   <- ZIO
                   .fromOption(jobState1.blobKey)
                   .orElseFail(PipelineError.DatabaseError(s"Missing blob key for job ${jobState1.id}", None))
      _           <- ZIO.logDebug(s"Job ${jobState1.id} - step 2: fetching blob $blobKey")
      contentType <-
        ZIO
          .fromOption(jobState1.mediaContentType)
          .orElseFail(PipelineError.DatabaseError(s"Missing media content type for job ${jobState1.id}", None))
      mediaFilename <-
        ZIO
          .fromOption(jobState1.mediaFilename)
          .orElseFail(PipelineError.DatabaseError(s"Missing media filename for job ${jobState1.id}", None))
      _               <- ZIO.logDebug(s"Job ${jobState1.id} - media: $mediaFilename (content-type: $contentType)")
      audio           <- blobStore.fetchAudio(blobKey)
      _               <- ZIO.logDebug(s"Job ${jobState1.id} - step 3: transcribing audio (size: ${audio.length} bytes)")
      temp_transcript <- transcriber
                           .transcribe(audio, contentType, mediaFilename)
      _ <-
        ZIO.logDebug(
          s"Job ${jobState1.id} - transcription completed: transcript ${temp_transcript.id}, text length: ${temp_transcript.text.length} chars"
        )
      transcript  = temp_transcript.addMetadatas(jobState1.metadata)
      _          <- ZIO.logDebug(s"Job ${jobState1.id} - step 4: persisting transcript ${transcript.id}")
      _          <- transcriptRepository.persist(transcript)
      jobWithTid <- jobRepository.update(
                      jobState1.copy(transcriptId = Some(transcript.id))
                    )
      _         <- ZIO.logDebug(s"Job ${jobWithTid.id} - step 5: marking as embedding")
      jobState2 <-
        jobRepository.update(
          jobWithTid.markEmbedding()
        )
      _         <- ZIO.logDebug(s"Job ${jobState2.id} - step 6: generating embeddings")
      segments  <- embedder.embed(transcript)
      _         <- ZIO.logDebug(s"Job ${jobState2.id} - embeddings generated: ${segments.size} chunks")
      _         <- ZIO.logDebug(s"Job ${jobState2.id} - step 7: marking as indexing")
      jobState3 <-
        jobRepository.update(
          jobState2.markIndexing()
        )
      chunkVectors        = segments.map(_._2)
      chunkTextsWithIndex = segments.zipWithIndex.map { case ((text, _), index) => (index, text) }
      _                  <- ZIO.logDebug(s"Job ${jobState3.id} - step 8: upserting ${chunkVectors.size} vectors into vector store")
      _                  <- vectorSink.upsertEmbeddings(transcript.id, chunkVectors, transcript.metadata)
      _                  <- ZIO.logDebug(s"Job ${jobState3.id} - vectors upserted successfully")
      _                  <- ZIO.logDebug(s"Job ${jobState3.id} - step 9: purging old lexical index")
      _                  <- lexicalStore
             .deleteTranscript(transcript.id)
             .catchAll(error => ZIO.logWarning(s"Failed to purge lexical index for ${transcript.id}: ${error.message}"))
      _ <-
        ZIO.logDebug(s"Job ${jobState3.id} - step 10: indexing ${chunkTextsWithIndex.size} segments into lexical store")
      _          <- lexicalStore.indexSegments(transcript.id, chunkTextsWithIndex, transcript.metadata)
      _          <- ZIO.logDebug(s"Job ${jobState3.id} - lexical index updated successfully")
      _          <- ZIO.logDebug(s"Job ${jobState3.id} - step 11: marking as success")
      finalState <-
        jobRepository.update(
          jobState3.markSuccess()
        )
      _ <- ZIO.logDebug(s"Job ${finalState.id} - step 12: deleting blob $blobKey")
      _ <- blobStore
             .deleteBlob(blobKey)
             .catchAll(error => ZIO.logWarning(s"Failed to delete blob for job ${finalState.id}: ${error.message}"))
      _ <- ZIO.logDebug(s"Job ${finalState.id} - processing completed successfully")
    } yield ()

  private def handleFailure(job: IngestionJob, error: PipelineError): ZIO[Any, Nothing, Unit] =
    for {
      _         <- ZIO.logDebug(s"Handling failure for job ${job.id}: ${error.message}")
      now       <- Clock.instant
      latestJob <-
        jobRepository
          .findById(job.id)
          .catchAll(err =>
            ZIO.logError(s"Failed to load job ${job.id} during failure handling: ${err.message}").as(Some(job))
          )
          .map(_.getOrElse(job))
      _ <-
        ZIO.logDebug(
          s"Job ${latestJob.id} current status: ${latestJob.status}, attempt: ${latestJob.attempt}/${latestJob.maxAttempts}"
        )
      _ <-
        if (latestJob.status == JobStatus.Success) {
          ZIO.logDebug(s"Job ${latestJob.id} already succeeded, skipping failure handling")
        } else {
          val retryAt    = computeNextRetry(latestJob, now)
          val updatedJob =
            if (retryAt.isDefined) latestJob.markFailed(error.message) else latestJob.markDeadLetter(error.message)
          for {
            _ <-
              ZIO.logDebug(
                s"Job ${latestJob.id} - computing retry: retryAt=${retryAt.map(_.toString).getOrElse("none")}, will mark as ${updatedJob.status}"
              )
            updateOk <- jobRepository
                          .update(updatedJob)
                          .as(true)
                          .catchAll(err =>
                            ZIO.logError(s"Failed to update job ${updatedJob.id} after error: ${err.message}").as(false)
                          )
            _ <- ZIO.logDebug(s"Job ${updatedJob.id} state update: ${if (updateOk) "success" else "failed"}")
            _ <-
              ZIO.logError(
                s"Ingestion job ${job.id} failed with '${error.message}'. Next retry at ${retryAt.map(_.toString).getOrElse("n/a")} (attempt ${updatedJob.attempt}/${updatedJob.maxAttempts})"
              )
            _ <- if (updateOk) {
                   retryAt match
                     case Some(next) =>
                       val delayMs = java.time.Duration.between(now, next).toMillis
                       ZIO.logDebug(s"Job ${job.id} - scheduling retry in ${delayMs}ms (at $next)") *>
                         jobQueue
                           .release(job.id) // Release from processing queue, will be re-enqueued
                           .catchAll(e => ZIO.logWarning(s"release failed for ${job.id}: ${e.message}")) *>
                         ZIO.sleep(zio.Duration.fromMillis(delayMs)) *>
                         jobQueue
                           .enqueue(job.id)
                           .catchAll(e => ZIO.logWarning(s"retry enqueue failed for ${job.id}: ${e.message}"))
                     case None =>
                       ZIO.logDebug(s"Job ${job.id} - max attempts reached, sending to dead letter queue") *>
                         jobQueue
                           .ack(job.id) // Remove from processing queue
                           .catchAll(e => ZIO.logWarning(s"ack failed for ${job.id}: ${e.message}")) *>
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
      _      <- ZIO.logDebug(s"Handling failure for job $jobId (by ID): ${error.message}")
      now    <- Clock.instant
      jobOpt <- jobRepository.findById(jobId).orElseSucceed(None)
      _      <- ZIO.logDebug(s"Job $jobId lookup during failure handling: ${
               if (jobOpt.isDefined) "found" else "not found, creating placeholder"
             }")
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
