package com.cyrelis.linguapipe.infrastructure.resilience

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.*
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.Transcript
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
    override def embed(transcript: Transcript): ZIO[Any, PipelineError, Array[Float]] = {
      val withTimeout = TimeoutService.applyEmbeddingTimeout(
        underlying.embed(transcript),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapDbSink(
    underlying: DbSinkPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): DbSinkPort = new DbSinkPort {
    override def persistTranscript(transcript: Transcript): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyDatabaseTimeout(
        underlying.persistTranscript(transcript),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapVectorSink(
    underlying: VectorSinkPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): VectorSinkPort = new VectorSinkPort {
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
    override def storeAudio(jobId: UUID, audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.storeAudio(jobId, audioContent, format),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def storeDocument(
      jobId: UUID,
      documentContent: String,
      mediaType: String
    ): ZIO[Any, PipelineError, Unit] = {
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
}
