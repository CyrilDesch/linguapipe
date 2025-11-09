package perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Feeder pour générer du contenu varié
  val textContentFeeder = Iterator.continually(
    Map(
      "content" -> s"This is a sample text content for testing purposes. Text number ${scala.util.Random.nextInt(1000)}. Lorem ipsum dolor sit amet, consectetur adipiscing elit."
    )
  )

  // Feeder pour les questions du RAG
  val queryFeeder = Iterator.continually(
    Map(
      "query" -> List(
        "What is the main topic?",
        "Can you summarize this content?",
        "What are the key points?",
        "Explain the details",
        "What information is available?"
      )(scala.util.Random.nextInt(5))
    )
  )

  val scn = scenario("Complete User Journey")
    .feed(textContentFeeder)
    // 1. Ingestion du contenu texte
    .exec(
      http("Ingest Text")
        .post("/api/v1/ingest/text")
        .body(StringBody("""{"content": "${content}"}"""))
        .asJson
        .check(status.saveAs("ingestStatusCode"))
        .check(bodyString.saveAs("ingestResponseBody"))
        .check(jsonPath("$.jobId").saveAs("jobId"))
    )
    .pause(1.second)

    // 2. Polling pour attendre que le job soit complété
    .asLongAs(session => session("jobStatus").asOption[String].getOrElse("pending") != "Success") {
      exec { session =>
        session.set("jobUrl", s"/api/v1/jobs/${session("jobId").as[String]}")
      }.exec(
        http("Check Job Status")
          .get("#{jobUrl}")
          .check(status.in(200, 404, 500).saveAs("statusCode"))
          .check(bodyString.saveAs("responseBody"))
          .checkIf((response, _) => response.status.code == 200) {
            jsonPath("$.status").optional.saveAs("jobStatus")
          }
      ).pause(2.seconds)
    }
    .pause(1.second)

    // 3. Vérification des transcriptions
    .exec(
      http("Get Transcripts")
        .get("/api/v1/transcripts")
        .check(status.is(200))
    )
    .pause(1.second)

    // 4. Plusieurs requêtes au RAG (3 à 5 questions)
    .repeat(_ => 3 + scala.util.Random.nextInt(3), "queryCount") {
      feed(queryFeeder)
        .exec(
          http("RAG Query ${queryCount}")
            .post("/api/v1/query")
            .body(StringBody("""{"query": "${query}"}"""))
            .asJson
            .check(status.is(200))
        )
        .pause(10.seconds, 1.minutes)
    }

  setUp(
    scn.inject(
      rampUsers(100).during(30.seconds)
    )
  ).protocols(httpProtocol)
    .maxDuration(5.minutes)
}
