package com.cyrelis.srag.infrastructure.resilience

import com.cyrelis.srag.application.errors.PipelineError
import zio.*

object ErrorMapper {

  def mapToPipelineError[R, A](
    effect: ZIO[R, Throwable, A],
    errorFactory: (String, Option[Throwable]) => PipelineError
  ): ZIO[R, PipelineError, A] =
    effect.mapError { throwable =>
      errorFactory(throwable.getMessage, Some(throwable))
    }

  def mapTranscriptionError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.TranscriptionError.apply)

  def mapEmbeddingError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.EmbeddingError.apply)

  def mapDatabaseError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.DatabaseError.apply)

  def mapVectorStoreError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.VectorStoreError.apply)

  def mapLexicalStoreError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.LexicalStoreError.apply)

  def mapBlobStoreError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.BlobStoreError.apply)

  def mapDocumentParserError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.DocumentParserError.apply)

  def mapRerankerError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, PipelineError, A] =
    mapToPipelineError(effect, PipelineError.RerankerError.apply)
}
