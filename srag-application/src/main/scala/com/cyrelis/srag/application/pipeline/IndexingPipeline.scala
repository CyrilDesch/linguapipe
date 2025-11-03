package com.cyrelis.srag.application.pipeline

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.srag.application.ports.driven.storage.{LexicalStorePort, VectorStorePort}
import com.cyrelis.srag.domain.ingestionjob.IngestionJob
import com.cyrelis.srag.domain.transcript.{Transcript, TranscriptRepository}
import zio.ZIO

final class IndexingPipeline(
  transcriptRepository: TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]],
  embedder: EmbedderPort,
  vectorSink: VectorStorePort,
  lexicalStore: LexicalStorePort
) {

  def index(transcript: Transcript, job: IngestionJob): ZIO[Any, PipelineError, Unit] =
    for {
      _                  <- ZIO.logDebug(s"Job ${job.id} - persisting transcript ${transcript.id}")
      _                  <- transcriptRepository.persist(transcript)
      _                  <- ZIO.logDebug(s"Job ${job.id} - generating embeddings")
      segments           <- embedder.embed(transcript)
      _                  <- ZIO.logDebug(s"Job ${job.id} - embeddings generated: ${segments.size} chunks")
      chunkVectors        = segments.map(_._2)
      chunkTextsWithIndex = segments.zipWithIndex.map { case ((text, _), index) => (index, text) }
      _                  <- ZIO.logDebug(s"Job ${job.id} - upserting ${chunkVectors.size} vectors into vector store")
      _                  <- vectorSink.upsertEmbeddings(transcript.id, chunkVectors, transcript.metadata)
      _                  <- ZIO.logDebug(s"Job ${job.id} - vectors upserted successfully")
      _                  <- ZIO.logDebug(s"Job ${job.id} - purging old lexical index")
      _                  <- lexicalStore
             .deleteTranscript(transcript.id)
             .catchAll(error => ZIO.logWarning(s"Failed to purge lexical index for ${transcript.id}: ${error.message}"))
      _ <- ZIO.logDebug(s"Job ${job.id} - indexing ${chunkTextsWithIndex.size} segments into lexical store")
      _ <- lexicalStore.indexSegments(transcript.id, chunkTextsWithIndex, transcript.metadata)
      _ <- ZIO.logDebug(s"Job ${job.id} - lexical index updated successfully")
    } yield ()
}
