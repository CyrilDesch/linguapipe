package com.cyrelis.srag.infrastructure.resilience

import java.util.UUID

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.srag.application.ports.driven.parser.DocumentParserPort
import com.cyrelis.srag.application.ports.driven.reranker.RerankerPort
import com.cyrelis.srag.application.ports.driven.storage.{BlobStorePort, LexicalStorePort, VectorStorePort}
import com.cyrelis.srag.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.srag.application.types.HealthStatus
import com.cyrelis.srag.domain.ingestionjob.{IngestionJob, IngestionJobRepository}
import com.cyrelis.srag.domain.transcript.{Transcript, TranscriptRepository}
import com.cyrelis.srag.infrastructure.config.{RetryConfig, TimeoutConfig}
import zio.*
import zio.stream.ZStream

object RetryWrappers {

  def wrapTranscriber(
    underlying: TranscriberPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): TranscriberPort = new TranscriberPort {
    override def transcribe(
      audioContent: Array[Byte],
      mediaContentType: String,
      mediaFilename: String
    ): ZIO[Any, PipelineError, Transcript] =
      RetryService.applyRetry(
        TimeoutService.applyTranscriptionTimeout(
          underlying.transcribe(audioContent, mediaContentType, mediaFilename),
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

    override def embedQuery(query: String): ZIO[Any, PipelineError, Array[Float]] = {
      val withTimeout = TimeoutService.applyEmbeddingTimeout(
        underlying.embedQuery(query),
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
    override def upsertEmbeddings(
      transcriptId: UUID,
      vectors: List[Array[Float]],
      metadata: Map[String, String]
    ): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyVectorStoreTimeout(
        underlying.upsertEmbeddings(transcriptId, vectors, metadata),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def searchSimilar(
      queryVector: Array[Float],
      limit: Int,
      filter: Option[com.cyrelis.srag.application.types.VectorStoreFilter]
    ): ZIO[Any, PipelineError, List[com.cyrelis.srag.application.types.VectorSearchResult]] = {
      val withTimeout = TimeoutService.applyVectorStoreTimeout(
        underlying.searchSimilar(queryVector, limit, filter),
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
    override def storeAudio(
      jobId: UUID,
      audioContent: Array[Byte],
      mediaContentType: String,
      mediaFilename: String
    ): ZIO[Any, PipelineError, String] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.storeAudio(jobId, audioContent, mediaContentType, mediaFilename),
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

    override def fetchBlobAsStream(blobKey: String): ZStream[Any, PipelineError, Byte] =
      underlying
        .fetchBlobAsStream(blobKey)
        .timeoutFail(
          PipelineError.BlobStoreError("Stream timeout", None)
        )(timeoutConfig.blobStoreMs.millis)

    override def getBlobFilename(blobKey: String): ZIO[Any, PipelineError, Option[String]] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.getBlobFilename(blobKey),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def getBlobContentType(blobKey: String): ZIO[Any, PipelineError, Option[String]] = {
      val withTimeout = TimeoutService.applyBlobStoreTimeout(
        underlying.getBlobContentType(blobKey),
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

  def wrapLexicalStore(
    underlying: LexicalStorePort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): LexicalStorePort = new LexicalStorePort {
    override def indexSegments(
      transcriptId: UUID,
      segments: List[(Int, String)],
      metadata: Map[String, String]
    ): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyLexicalStoreTimeout(
        underlying.indexSegments(transcriptId, segments, metadata),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def deleteTranscript(transcriptId: UUID): ZIO[Any, PipelineError, Unit] = {
      val withTimeout = TimeoutService.applyLexicalStoreTimeout(
        underlying.deleteTranscript(transcriptId),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def search(
      queryText: String,
      limit: Int,
      filter: Option[com.cyrelis.srag.application.types.VectorStoreFilter]
    ): ZIO[Any, PipelineError, List[com.cyrelis.srag.application.types.LexicalSearchResult]] = {
      val withTimeout = TimeoutService.applyLexicalStoreTimeout(
        underlying.search(queryText, limit, filter),
        timeoutConfig
      )
      RetryService.applyRetry(withTimeout, retryConfig)
    }

    override def healthCheck(): Task[HealthStatus] =
      underlying.healthCheck()
  }

  def wrapReranker(
    underlying: RerankerPort,
    retryConfig: RetryConfig,
    timeoutConfig: TimeoutConfig
  ): RerankerPort = new RerankerPort {
    override def rerank(
      query: String,
      candidates: List[com.cyrelis.srag.application.types.RerankerCandidate],
      topK: Int
    ): ZIO[Any, PipelineError, List[com.cyrelis.srag.application.types.RerankerResult]] = {
      val withTimeout = TimeoutService.applyRerankerTimeout(
        underlying.rerank(query, candidates, topK),
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
