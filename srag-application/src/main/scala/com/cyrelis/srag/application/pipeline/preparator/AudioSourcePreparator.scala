package com.cyrelis.srag.application.pipeline

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.ports.driven.storage.BlobStorePort
import com.cyrelis.srag.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.srag.domain.ingestionjob.IngestionJob
import com.cyrelis.srag.domain.transcript.Transcript
import zio.ZIO

final class AudioSourcePreparator(
  blobStore: BlobStorePort,
  transcriber: TranscriberPort
) extends SourcePreparator {

  override def prepare(job: IngestionJob): ZIO[Any, PipelineError, Transcript] =
    for {
      blobKey <- ZIO
                   .fromOption(job.blobKey)
                   .orElseFail(PipelineError.DatabaseError(s"Missing blob key for job ${job.id}", None))
      _           <- ZIO.logDebug(s"Job ${job.id} - fetching blob $blobKey")
      contentType <-
        ZIO
          .fromOption(job.mediaContentType)
          .orElseFail(PipelineError.DatabaseError(s"Missing media content type for job ${job.id}", None))
      mediaFilename <-
        ZIO
          .fromOption(job.mediaFilename)
          .orElseFail(PipelineError.DatabaseError(s"Missing media filename for job ${job.id}", None))
      _               <- ZIO.logDebug(s"Job ${job.id} - media: $mediaFilename (content-type: $contentType)")
      audio           <- blobStore.fetchAudio(blobKey)
      _               <- ZIO.logDebug(s"Job ${job.id} - transcribing audio (size: ${audio.length} bytes)")
      temp_transcript <- transcriber.transcribe(audio, contentType, mediaFilename)
      _               <-
        ZIO.logDebug(
          s"Job ${job.id} - transcription completed: transcript ${temp_transcript.id}, text length: ${temp_transcript.text.length} chars"
        )
      transcript = temp_transcript.addMetadatas(job.metadata)
    } yield transcript
}
