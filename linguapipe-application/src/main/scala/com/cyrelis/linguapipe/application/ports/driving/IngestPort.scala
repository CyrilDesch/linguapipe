package com.cyrelis.linguapipe.application.ports.driving

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.domain.ingestionjob.IngestionJob
import zio.*

trait IngestPort {
  def submitAudio(
    audioContent: Array[Byte],
    mediaContentType: String,
    mediaFilename: String,
    metadata: Map[String, String]
  ): ZIO[Any, PipelineError, IngestionJob]
  def submitText(textContent: String, metadata: Map[String, String]): ZIO[Any, PipelineError, IngestionJob]
  def submitDocument(
    documentContent: String,
    mediaType: String,
    metadata: Map[String, String]
  ): ZIO[Any, PipelineError, IngestionJob]
  def getJob(jobId: UUID): ZIO[Any, PipelineError, Option[IngestionJob]]
}
