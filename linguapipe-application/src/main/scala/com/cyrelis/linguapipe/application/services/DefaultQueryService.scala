package com.cyrelis.linguapipe.application.services

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.storage.VectorStorePort
import com.cyrelis.linguapipe.application.types.{ContextSegment, VectorStoreFilter}
import com.cyrelis.linguapipe.domain.transcript.TranscriptRepository
import zio.*

final class DefaultQueryService(
  embedder: EmbedderPort,
  vectorStore: VectorStorePort,
  transcriptRepository: TranscriptRepository[[X] =>> ZIO[Any, PipelineError, X]]
) {

  def retrieveContext(
    queryText: String,
    filter: Option[VectorStoreFilter],
    limit: Int = 5
  ): ZIO[Any, PipelineError, List[ContextSegment]] =
    for {
      // Step 1: Embed the query text
      queryVector <- embedder.embedQuery(queryText)

      // Step 2: Search similar vectors in the vector store
      searchResults <- vectorStore.searchSimilar(queryVector, limit, filter)

      // Step 2: Retrieve transcripts for the search results
      transcriptIds = searchResults.map(_.transcriptId).distinct
      transcripts  <- ZIO.foreach(transcriptIds)(id =>
                       transcriptRepository
                         .getById(id)
                         .map(
                           _.getOrElse(
                             throw new RuntimeException(s"Transcript $id not found")
                           )
                         )
                     )

      // Step 3: Extract relevant segments from transcripts
      // For each search result, get the corresponding chunk from the transcript
      // Note: We re-chunk using the same logic as during embedding (1000 tokens per chunk)
      contextSegments <- ZIO.succeed(
                           searchResults.flatMap { result =>
                             transcripts
                               .find(_.id == result.transcriptId)
                               .map { (transcript: com.cyrelis.linguapipe.domain.transcript.Transcript) =>
                                 // Re-chunk the transcript to get the chunk at the given index
                                 // This matches the chunking done during embedding
                                 val chunks = DefaultQueryService.TextChunker.chunkText(transcript.text, 1000)
                                 chunks.lift(result.segmentIndex).map { text =>
                                   ContextSegment(
                                     transcriptId = result.transcriptId,
                                     segmentIndex = result.segmentIndex,
                                     text = text,
                                     score = result.score
                                   )
                                 }
                               }
                               .flatten
                           }
                         )
    } yield contextSegments
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
