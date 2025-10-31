package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.endpoint

import com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.main.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object MainEndpoints {

  val health: PublicEndpoint[Unit, String, List[HealthStatusRestDto], Any] =
    sttp.tapir.endpoint.get
      .in("health")
      .out(jsonBody[List[HealthStatusRestDto]])
      .errorOut(stringBody)
      .description("Health check endpoint")

  val ingestAudioMultipart: PublicEndpoint[AudioIngestMultipartDto, String, JobAcceptedRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "audio")
      .in(multipartBody[AudioIngestMultipartDto])
      .out(statusCode(StatusCode.Accepted))
      .out(jsonBody[JobAcceptedRestDto])
      .errorOut(stringBody)
      .description("Ingest audio file directly (multipart/form-data)")

  val ingestText: PublicEndpoint[TextIngestRestDto, String, JobAcceptedRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "text")
      .in(jsonBody[TextIngestRestDto])
      .out(statusCode(StatusCode.Accepted))
      .out(jsonBody[JobAcceptedRestDto])
      .errorOut(stringBody)
      .description("Ingest raw text content")

  val ingestDocument: PublicEndpoint[DocumentIngestRestDto, String, JobAcceptedRestDto, Any] =
    sttp.tapir.endpoint.post
      .in("api" / "v1" / "ingest" / "document")
      .in(jsonBody[DocumentIngestRestDto])
      .out(statusCode(StatusCode.Accepted))
      .out(jsonBody[JobAcceptedRestDto])
      .errorOut(stringBody)
      .description("Ingest base64-encoded document content")

  val jobStatus: PublicEndpoint[String, String, JobStatusRestDto, Any] =
    sttp.tapir.endpoint.get
      .in("api" / "v1" / "jobs" / path[String]("jobId"))
      .out(jsonBody[JobStatusRestDto])
      .errorOut(stringBody)
      .description("Get ingestion job status by job id")

  val transcripts
    : PublicEndpoint[(TranscriptsQueryRestDto, sttp.model.QueryParams), String, List[TranscriptRestDto], Any] =
    sttp.tapir.endpoint.get
      .in("api" / "v1" / "transcripts")
      .in(query[Option[String]]("sortBy").description("createdAt|metadata (default: createdAt)"))
      .in(query[Option[String]]("metadataSort").description("Required when sortBy=metadata"))
      .in(query[Option[String]]("order").description("asc|desc (default: asc)"))
      .in(
        query[List[String]]("metadata").description(
          "Repeatable: key=value; e.g., metadata=project=alpha&metadata=userId=42"
        )
      )
      .mapInTo[TranscriptsQueryRestDto]
      .in(queryParams)
      .out(jsonBody[List[TranscriptRestDto]])
      .errorOut(stringBody)
      .description(
        """
        List transcripts.

        Filtering:
        - metadata=<key>=<value> (repeatable) OR metadata.<key>=<value>

        Sorting:
        - sortBy=createdAt|metadata (default: createdAt)
        - metadataSort=<key> (required when sortBy=metadata)
        - order=asc|desc (default: asc)
        """.stripMargin
      )

  def all: List[PublicEndpoint[?, ?, ?, ?]] =
    List(health, ingestAudioMultipart, ingestText, ingestDocument, jobStatus, transcripts)
}
