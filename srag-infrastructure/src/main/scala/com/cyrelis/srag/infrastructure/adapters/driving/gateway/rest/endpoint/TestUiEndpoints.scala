package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.endpoint

import com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.testui.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object TestUiEndpoints {

  val listAllJobs: PublicEndpoint[Unit, String, List[AdminJobDto], Any] =
    sttp.tapir.endpoint.get
      .in("test-ui" / "jobs")
      .out(jsonBody[List[AdminJobDto]])
      .errorOut(stringBody)
      .description("List all ingestion jobs (test UI only)")

  val listAllVectors: PublicEndpoint[Unit, String, AdminVectorsResponse, Any] =
    sttp.tapir.endpoint.get
      .in("test-ui" / "vectors")
      .out(jsonBody[AdminVectorsResponse])
      .errorOut(stringBody)
      .description("List all vectors from Qdrant (test UI only)")

  val listAllBlobs: PublicEndpoint[Unit, String, List[AdminBlobDto], Any] =
    sttp.tapir.endpoint.get
      .in("test-ui" / "blobs")
      .out(jsonBody[List[AdminBlobDto]])
      .errorOut(stringBody)
      .description("List all blobs from MinIO (test UI only)")

  val listAllOpenSearch: PublicEndpoint[Unit, String, AdminOpenSearchResponse, Any] =
    sttp.tapir.endpoint.get
      .in("test-ui" / "opensearch")
      .out(jsonBody[AdminOpenSearchResponse])
      .errorOut(stringBody)
      .description("List all documents from OpenSearch (test UI only)")

  def all: List[PublicEndpoint[?, ?, ?, ?]] =
    List(
      listAllJobs,
      listAllVectors,
      listAllBlobs,
      listAllOpenSearch
    )
}
