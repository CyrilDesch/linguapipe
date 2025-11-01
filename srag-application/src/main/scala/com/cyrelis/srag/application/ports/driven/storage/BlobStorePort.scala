package com.cyrelis.srag.application.ports.driven.storage

import java.util.UUID

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.HealthStatus
import zio.*
import zio.stream.ZStream

trait BlobStorePort {
  def storeAudio(
    jobId: UUID,
    audioContent: Array[Byte],
    mediaContentType: String,
    mediaFilename: String
  ): ZIO[Any, PipelineError, String]
  def fetchAudio(blobKey: String): ZIO[Any, PipelineError, Array[Byte]]
  def fetchBlobAsStream(blobKey: String): ZStream[Any, PipelineError, Byte]
  def getBlobFilename(blobKey: String): ZIO[Any, PipelineError, Option[String]]
  def getBlobContentType(blobKey: String): ZIO[Any, PipelineError, Option[String]]
  def deleteBlob(blobKey: String): ZIO[Any, PipelineError, Unit]
  def storeDocument(jobId: UUID, documentContent: String, mediaType: String): ZIO[Any, PipelineError, String]
  def healthCheck(): Task[HealthStatus]
}
