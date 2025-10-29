package com.cyrelis.linguapipe.infrastructure.resilience

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.infrastructure.config.TimeoutConfig
import zio.*

object TimeoutService {

  def applyTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    timeoutMs: Long,
    operationName: String
  ): ZIO[R, PipelineError, A] =
    effect.timeout(timeoutMs.millis).flatMap {
      case Some(result) => ZIO.succeed(result)
      case None         =>
        ZIO.fail(
          PipelineError.TimeoutError(
            operation = operationName,
            timeoutMs = timeoutMs,
            cause = None
          )
        )
    }

  def applyTranscriptionTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    config: TimeoutConfig
  ): ZIO[R, PipelineError, A] =
    applyTimeout(effect, config.transcriptionMs, "transcription")

  def applyEmbeddingTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    config: TimeoutConfig
  ): ZIO[R, PipelineError, A] =
    applyTimeout(effect, config.embeddingMs, "embedding")

  def applyDatabaseTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    config: TimeoutConfig
  ): ZIO[R, PipelineError, A] =
    applyTimeout(effect, config.databaseMs, "database")

  def applyVectorStoreTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    config: TimeoutConfig
  ): ZIO[R, PipelineError, A] =
    applyTimeout(effect, config.vectorStoreMs, "vector_store")

  def applyBlobStoreTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    config: TimeoutConfig
  ): ZIO[R, PipelineError, A] =
    applyTimeout(effect, config.blobStoreMs, "blob_store")

  def applyDocumentParserTimeout[R, A](
    effect: ZIO[R, PipelineError, A],
    config: TimeoutConfig
  ): ZIO[R, PipelineError, A] =
    applyTimeout(effect, config.documentParserMs, "document_parser")
}
