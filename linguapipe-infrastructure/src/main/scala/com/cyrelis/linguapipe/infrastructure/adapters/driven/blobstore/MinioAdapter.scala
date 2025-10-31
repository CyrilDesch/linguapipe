package com.cyrelis.linguapipe.infrastructure.adapters.driven.blobstore

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.Instant
import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.storage.BlobStorePort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.resilience.ErrorMapper
import zio.*

final class MinioAdapter(endpoint: String, accessKey: String, secretKey: String, bucket: String) extends BlobStorePort {

  private val storageRoot: Path = Paths.get(java.lang.System.getProperty("java.io.tmpdir"), "linguapipe", bucket)

  override def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, String] =
    ErrorMapper.mapBlobStoreError {
      for {
        key  <- ZIO.succeed(s"$jobId/audio-${java.lang.System.currentTimeMillis()}.$format")
        path <- resolveKey(key)
        _    <- ensureParentExists(path)
        _    <-
          ZIO.attempt(Files.write(path, audioContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
      } yield key
    }

  override def fetchAudio(blobKey: String): ZIO[Any, PipelineError, Array[Byte]] =
    ErrorMapper.mapBlobStoreError {
      for {
        path  <- resolveKey(blobKey)
        bytes <- ZIO.attempt(Files.readAllBytes(path))
      } yield bytes
    }

  override def deleteBlob(blobKey: String): ZIO[Any, PipelineError, Unit] =
    ErrorMapper.mapBlobStoreError {
      resolveKey(blobKey).flatMap { path =>
        ZIO.attempt(Files.deleteIfExists(path)).unit
      }
    }

  override def storeDocument(jobId: UUID, documentContent: String, mediaType: String): ZIO[Any, PipelineError, String] =
    ErrorMapper.mapBlobStoreError {
      for {
        key  <- ZIO.succeed(s"$jobId/document-${java.lang.System.currentTimeMillis()}.$mediaType")
        path <- resolveKey(key)
        _    <- ensureParentExists(path)
        _    <- ZIO.attempt(
               Files.writeString(path, documentContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
             )
      } yield key
    }

  override def healthCheck(): Task[HealthStatus] =
    ZIO.succeed(
      HealthStatus.Healthy(
        serviceName = s"Minio($endpoint)",
        checkedAt = Instant.now(),
        details = Map("endpoint" -> endpoint, "accessKey" -> accessKey, "secretKey" -> secretKey, "bucket" -> bucket)
      )
    )

  private def resolveKey(key: String): Task[Path] =
    ZIO.succeed(storageRoot.resolve(key))

  private def ensureParentExists(path: Path): Task[Unit] =
    ZIO.attempt(Files.createDirectories(path.getParent)).unit
}
