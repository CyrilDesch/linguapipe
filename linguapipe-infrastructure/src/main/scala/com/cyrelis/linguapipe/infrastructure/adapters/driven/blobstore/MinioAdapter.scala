package com.cyrelis.linguapipe.infrastructure.adapters.driven.blobstore

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID

import scala.jdk.CollectionConverters.*

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.storage.BlobStorePort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.resilience.ErrorMapper
import io.minio.*
import zio.*
import zio.stream.ZStream

final class MinioAdapter(host: String, port: Int, accessKey: String, secretKey: String, bucket: String)
    extends BlobStorePort {

  private val endpoint: String = s"$host:$port"

  private lazy val minioClient: MinioClient =
    try {
      MinioClient
        .builder()
        .endpoint(host, port, false)
        .credentials(accessKey, secretKey)
        .build()
    } catch {
      case e: IllegalArgumentException =>
        throw new IllegalArgumentException(s"Invalid MinIO configuration: ${e.getMessage}", e)
      case e: Throwable => throw e
    }

  override def storeAudio(
    jobId: UUID,
    audioContent: Array[Byte],
    mediaContentType: String,
    mediaFilename: String
  ): ZIO[Any, PipelineError, String] =
    ErrorMapper.mapBlobStoreError {
      for {
        blobKey <- ZIO.succeed(UUID.randomUUID().toString)
        metadata = Map(
                     "x-amz-meta-original-filename" -> mediaFilename,
                     "x-amz-meta-content-type"      -> mediaContentType
                   ).asJava
        _ <- uploadBlobWithMetadata(blobKey, audioContent, mediaContentType, metadata)
      } yield blobKey
    }

  override def fetchAudio(blobKey: String): ZIO[Any, PipelineError, Array[Byte]] =
    ErrorMapper.mapBlobStoreError {
      for {
        inputStream <-
          ZIO.attempt(minioClient.getObject(GetObjectArgs.builder().bucket(bucket).`object`(blobKey).build()))
        bytes <- ZIO.attempt(inputStream.readAllBytes()).ensuring(ZIO.attempt(inputStream.close()).ignore)
      } yield bytes
    }

  override def fetchBlobAsStream(blobKey: String): ZStream[Any, PipelineError, Byte] =
    ZStream
      .fromZIO(
        ErrorMapper.mapBlobStoreError(
          ZIO.attempt(minioClient.getObject(GetObjectArgs.builder().bucket(bucket).`object`(blobKey).build()))
        )
      )
      .flatMap(inputStream =>
        ZStream
          .fromInputStream(inputStream)
          .mapError(ioe => PipelineError.BlobStoreError(ioe.getMessage, Some(ioe)))
          .ensuring(ZIO.attempt(inputStream.close()).ignore)
      )

  override def getBlobFilename(blobKey: String): ZIO[Any, PipelineError, Option[String]] =
    ErrorMapper.mapBlobStoreError {
      ZIO.attempt {
        val statObjectResponse = minioClient.statObject(
          StatObjectArgs.builder().bucket(bucket).`object`(blobKey).build()
        )
        val metadata = statObjectResponse.userMetadata()
        // MinIO/S3 may normalize metadata keys to lowercase
        // Try exact key first, then search case-insensitively
        val exactKey = "x-amz-meta-original-filename"
        Option(metadata.get(exactKey)).orElse {
          // Search for key containing "original-filename" case-insensitively
          metadata.asScala.collectFirst {
            case (k, v) if k.toLowerCase.contains("original-filename") => v
          }
        }
      }
    }

  override def getBlobContentType(blobKey: String): ZIO[Any, PipelineError, Option[String]] =
    ErrorMapper.mapBlobStoreError {
      ZIO.attempt {
        val statObjectResponse = minioClient.statObject(
          StatObjectArgs.builder().bucket(bucket).`object`(blobKey).build()
        )
        val metadata = statObjectResponse.userMetadata()
        // MinIO/S3 may normalize metadata keys to lowercase
        // Try exact key first, then search case-insensitively
        val exactKey = "x-amz-meta-content-type"
        Option(metadata.get(exactKey)).orElse {
          // Search for key containing "content-type" case-insensitively
          metadata.asScala.collectFirst {
            case (k, v) if k.toLowerCase.contains("content-type") => v
          }
        }.orElse {
          // Fallback to the object's Content-Type if metadata not found
          Option(statObjectResponse.contentType())
        }
      }
    }

  override def deleteBlob(blobKey: String): ZIO[Any, PipelineError, Unit] =
    ErrorMapper.mapBlobStoreError {
      ZIO.attempt(minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(blobKey).build())).unit
    }

  override def storeDocument(jobId: UUID, documentContent: String, mediaType: String): ZIO[Any, PipelineError, String] =
    ErrorMapper.mapBlobStoreError {
      for {
        blobKey <- ZIO.succeed(UUID.randomUUID().toString)
        filename = s"document-${java.lang.System.currentTimeMillis()}.$mediaType"
        metadata = Map(
                     "x-amz-meta-original-filename" -> filename,
                     "x-amz-meta-content-type"      -> mediaType
                   ).asJava
        contentBytes <- ZIO.succeed(documentContent.getBytes("UTF-8"))
        _            <- uploadBlobWithMetadata(blobKey, contentBytes, mediaType, metadata)
      } yield blobKey
    }

  override def healthCheck(): Task[HealthStatus] =
    (ZIO.attempt(minioClient.listBuckets()) catchAll { t =>
      ZIO.fail(t)
    }).map { _ =>
      HealthStatus.Healthy(
        serviceName = s"MinIO($endpoint)",
        checkedAt = Instant.now(),
        details = Map("endpoint" -> endpoint, "bucket" -> bucket)
      )
    }.catchAll { error =>
      ZIO.succeed(
        HealthStatus.Unhealthy(
          serviceName = s"MinIO($endpoint)",
          checkedAt = Instant.now(),
          error = error.getMessage,
          details = Map("endpoint" -> endpoint, "bucket" -> bucket)
        )
      )
    }

  private def uploadBlobWithMetadata(
    key: String,
    content: Array[Byte],
    contentType: String,
    metadata: java.util.Map[String, String]
  ): Task[Unit] =
    ZIO.attempt {
      val inputStream = ByteArrayInputStream(content)
      minioClient.putObject(
        PutObjectArgs
          .builder()
          .bucket(bucket)
          .`object`(key)
          .stream(inputStream, content.length, -1)
          .contentType(contentType)
          .userMetadata(metadata)
          .build()
      )
      inputStream.close()
    }

}
