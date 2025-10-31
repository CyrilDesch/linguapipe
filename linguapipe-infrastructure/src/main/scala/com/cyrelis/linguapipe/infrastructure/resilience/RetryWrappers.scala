package com.cyrelis.linguapipe.infrastructure.resilience

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.ingestionjob.{IngestionJob, IngestionJobRepository}
import com.cyrelis.linguapipe.domain.transcript.{Transcript, TranscriptRepository}
import com.cyrelis.linguapipe.infrastructure.config.{RetryConfig, TimeoutConfig}
import zio.*

object RetryWrappers {

  def wrapTranscriber(
    underlying: TranscriberPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): TranscriberPort = new TranscriberPort {
    override def transcribe(audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Transcript] =
      RetryService.applyRetry(
        TimeoutService.applyTranscriptionTimeout(
          underlying.transcribe(audioContent, format),
          timeoutConfig
        ),
        retryConfig
      )

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapEmbedder(
    underlying: EmbedderPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): EmbedderPort = new EmbedderPort {
    override def embed(transcript: Transcript): ZIO[Any, PipelineError, List[(String, Array[Float])]] = {
      val withTimeout = TimeoutService.applyEmbeddingTimeout(
        underlying.embed(transcript),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapTranscriptRepository(
    underlying: TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]],
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]] =
    new TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]] {
      override def persist(transcript: Transcript): ZIO[Any, PipelineError, Unit] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.persist(transcript),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }

      override def getAll(): ZIO[Any, PipelineError, List[Transcript]] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.getAll(),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }

      override def getById(id: java.util.UUID): ZIO[Any, PipelineError, Option[Transcript]] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.getById(id),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }
    }

  def wrapVectorSink(
    underlying: VectorStorePort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): VectorStorePort = new VectorStorePort {
    override def upsertEmbeddings(transcriptId: UUID, vectors: List[Array[Float]]): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyVectorStoreTimeout(
        underlying.upsertEmbeddings(transcriptId, vectors),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapBlobStore(
    underlying: BlobStorePort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): BlobStorePort = new BlobStorePort {
    override def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, String] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.storeAudio(jobId, audioContent, format),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def fetchAudio(blobKey: String): ZIO[Any, PipelineError, Array[Byte]] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.fetchAudio(blobKey),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def deleteBlob(blobKey: String): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.deleteBlob(blobKey),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def storeDocument(
      jobId: UUID,
      documentContent: String,
      mediaType: String
    ): ZIO[Any, PipelineError, String] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.storeDocument(jobId, documentContent, mediaType),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapDocumentParser(
    underlying: DocumentParserPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): DocumentParserPort = new DocumentParserPort {
    override def parseDocument(content: String, mediaType: String): ZIO[Any, PipelineError, String] = {
      val withTimeout = TimeoutService.applyDocumentParserTimeout(
        underlying.parseDocument(content, mediaType),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }
  }

  def wrapJobRepository(
    underlying: IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]],
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]] =
    new IngestionJobRepository[[X] =>> ZIO[Any, PipelineError, X]] {
      override def create(job: IngestionJob): ZIO[Any, PipelineError, IngestionJob] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.create(job),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }

      override def update(job: IngestionJob): ZIO[Any, PipelineError, IngestionJob] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.update(job),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }

      override def findById(jobId: UUID): ZIO[Any, PipelineError, Option[IngestionJob]] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.findById(jobId),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }

      override def listRunnable(now: java.time.Instant, limit: Int): ZIO[Any, PipelineError, List[IngestionJob]] = {
        val withTimeout = TimeoutService.applyDatabaseTimeout(
          underlying.listRunnable(now, limit),
          timeoutConfig
        )
        RetryService.applyRetry(withTimeout, retryConfig)
      }
    }
}
