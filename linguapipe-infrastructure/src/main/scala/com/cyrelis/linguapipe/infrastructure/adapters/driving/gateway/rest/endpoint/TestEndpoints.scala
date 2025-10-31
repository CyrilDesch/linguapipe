package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.endpoint

import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.test.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object TestEndpoints {

  val testTranscriber: PublicEndpoint[TestTranscriberRestDto, String, TestTranscriberResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "transcriber")
      .in(multipartBody[TestTranscriberRestDto])
      .out(jsonBody[TestTranscriberResultRestDto])
      .errorOut(stringBody)
      .description("Test transcriber adapter with multipart audio file")

  val testEmbedder: PublicEndpoint[TestEmbedderRestDto, String, TestEmbedderResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "embedder")
      .in(jsonBody[TestEmbedderRestDto])
      .out(jsonBody[TestEmbedderResultRestDto])
      .errorOut(stringBody)
      .description("Test embedder adapter with text")

  val testBlobStore: PublicEndpoint[TestBlobStoreRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "blobstore")
      .in(multipartBody[TestBlobStoreRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test blob store adapter with multipart audio file")

  val testDocumentParser: PublicEndpoint[TestDocumentParserRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "documentparser")
      .in(jsonBody[TestDocumentParserRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test document parser adapter")

  val testDatabase: PublicEndpoint[TestDatabaseRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "database")
      .in(jsonBody[TestDatabaseRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test database adapter")

  val testVectorStore: PublicEndpoint[TestVectorStoreRestDto, String, TestResultRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("test" / "vectorstore")
      .in(jsonBody[TestVectorStoreRestDto])
      .out(jsonBody[TestResultRestDto])
      .errorOut(stringBody)
      .description("Test vector store adapter")

  val getAllTranscripts: PublicEndpoint[Unit, String, List[TranscriptResponseDto], Any] =
    sttp.tapir.endpoint.get
      .in("test" / "transcripts")
      .out(jsonBody[List[TranscriptResponseDto]])
      .errorOut(stringBody)
      .description("Get all transcripts from database")

  def all: List[PublicEndpoint[?, ?, ?, ?]] =
    List(
      testTranscriber,
      testEmbedder,
      testBlobStore,
      testDocumentParser,
      testDatabase,
      testVectorStore,
      getAllTranscripts
    )
}
