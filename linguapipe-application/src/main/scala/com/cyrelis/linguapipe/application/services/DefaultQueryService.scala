package com.cyrelis.linguapipe.application.services

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.reranker.RerankerPort
import com.cyrelis.linguapipe.application.ports.driven.storage.{LexicalStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.types.{ContextSegment, LexicalSearchResult, RerankerCandidate, RerankerResult, VectorSearchResult, VectorStoreFilter}
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import zio.*

final class DefaultQueryService(
  embedder: EmbedderPort,
  vectorStore: VectorStorePort,
  lexicalStore: LexicalStorePort,
  reranker: RerankerPort,
  transcriptRepository: TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]
) {

  private val fusionPoolSize   = 200
  private val rerankerPoolSize = 200
  private val rrfK             = 60

  def retrieveContext(
    queryText: String,
    filter: Option[VectorStoreFilter],
    limit: Int = 5
  ): ZIO[Any, PipelineError, List[ContextSegment]] =
    for {
      // Step 1: Embed the query text
      queryVector <- embedder.embedQuery(queryText)

      // Step 2: Query both semantic and lexical stores with generous candidate pool sizes
      semanticResults <- vectorStore.searchSimilar(queryVector, fusionPoolSize, filter)
      lexicalResults  <- lexicalStore.search(queryText, fusionPoolSize, filter)

      fusedCandidates = fuseCandidates(semanticResults, lexicalResults)

      result <- if (fusedCandidates.isEmpty) ZIO.succeed(List.empty[ContextSegment])
                else buildContextSegments(queryText, fusedCandidates, lexicalResults, limit)
    } yield result

  private type CandidateKey = (UUID, Int)

  private def fuseCandidates(
    semantic: List[VectorSearchResult],
    lexical: List[LexicalSearchResult]
  ): List[(CandidateKey, Double)] = {
    val semanticRanked = semantic.sortBy(result => -result.score).zipWithIndex.map { case (result, idx) =>
      ((result.transcriptId, result.segmentIndex), idx + 1)
    }.toMap

    val lexicalRanked = lexical.sortBy(result => -result.score).zipWithIndex.map { case (result, idx) =>
      ((result.transcriptId, result.segmentIndex), idx + 1)
    }.toMap

    val allKeys = semanticRanked.keySet ++ lexicalRanked.keySet

    allKeys.toList
      .map { key =>
        val semanticScore = semanticRanked.get(key).map(rank => 1.0 / (rrfK + rank)).getOrElse(0.0)
        val lexicalScore  = lexicalRanked.get(key).map(rank => 1.0 / (rrfK + rank)).getOrElse(0.0)
        key -> (semanticScore + lexicalScore)
      }
      .filter(_._2 > 0.0)
      .sortBy { case (_, score) => -score }
      .take(fusionPoolSize)
  }

  private def buildContextSegments(
    queryText: String,
    fused: List[(CandidateKey, Double)],
    lexicalResults: List[LexicalSearchResult],
    limit: Int
  ): ZIO[Any, PipelineError, List[ContextSegment]] = {
    val transcriptIds = fused.map(_._1._1).distinct

    for {
      transcripts <- ZIO
                       .foreach(transcriptIds)(id => transcriptRepository.getById(id).map(_.map(id -> _)))
      transcriptMap = transcripts.collect { case Some(value) => value }.toMap
      chunkIndex     = buildChunkIndex(transcriptMap.values.toList)
      lexicalIndex   = lexicalResults.map(result => ((result.transcriptId, result.segmentIndex), result.text)).toMap
      candidatesWithScores = fused.flatMap { case (key, fusedScore) =>
                               val text = lexicalIndex.get(key).filter(_.nonEmpty).orElse(chunkIndex.get(key))
                               text.map { chunkText =>
                                 (RerankerCandidate(key._1, key._2, chunkText), fusedScore)
                               }
                             }
      fallback = candidatesWithScores
                   .sortBy { case (_, fusedScore) => -fusedScore }
                   .take(limit)
                   .map { case (candidate, fusedScore) =>
                     ContextSegment(
                       transcriptId = candidate.transcriptId,
                       segmentIndex = candidate.segmentIndex,
                       text = candidate.text,
                       score = fusedScore
                     )
                   }
      finalResult <- if (candidatesWithScores.isEmpty) ZIO.succeed(fallback)
                     else {
                       val candidatesOnly = candidatesWithScores.map(_._1)
                       val rerankTopK     = math.min(rerankerPoolSize, candidatesOnly.size)
                       reranker
                         .rerank(queryText, candidatesOnly, rerankTopK)
                         .either
                         .flatMap {
                           case Right(reranked) if reranked.nonEmpty =>
                             ZIO.succeed(
                               reranked
                                 .sortBy(res => -res.score)
                                 .take(limit)
                                 .map(toContextSegment)
                             )
                           case Right(_) => ZIO.succeed(fallback)
                           case Left(error) =>
                             ZIO.logWarning(s"Reranker failed: ${error.message}") *> ZIO.succeed(fallback)
                         }
                     }
    } yield finalResult
  }

  private def buildChunkIndex(
    transcripts: List[com.cyrelis.linguapipe.domain.transcript.Transcript]
  ): Map[CandidateKey, String] =
    transcripts.flatMap { transcript =>
      val chunks = DefaultQueryService.TextChunker.chunkText(transcript.text, 1000)
      chunks.zipWithIndex.map { case (chunk, idx) => (transcript.id -> idx) -> chunk }
    }.toMap

  private def toContextSegment(result: RerankerResult): ContextSegment =
    ContextSegment(
      transcriptId = result.candidate.transcriptId,
      segmentIndex = result.candidate.segmentIndex,
      text = result.candidate.text,
      score = result.score
    )
}

object DefaultQueryService {

  // Helper to chunk text (matching the logic used during embedding)
  // In production, this chunking logic should be shared between embedder and this service
  object TextChunker {
    def chunkText(text: String, chunkSize: Int): List[String] = {
      val words = text.split("\\s+").toList
      words.grouped(chunkSize).map(_.mkString(" ")).toList
    }
  }
}
