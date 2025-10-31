package com.cyrelis.linguapipe.application.ports.driven.storage

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

trait BlobStorePort {
  def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, String]
  def fetchAudio(blobKey: String): ZIO[Any, PipelineError, Array[Byte]]
  def deleteBlob(blobKey: String): ZIO[Any, PipelineError, Unit]
  def storeDocument(jobId: UUID, documentContent: String, mediaType: String): ZIO[Any, PipelineError, String]
  def healthCheck(): Task[HealthStatus]
}
