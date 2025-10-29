package com.cyrelis.linguapipe.infrastructure.adapters.driven.database.postgres

import java.time.Instant
import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.{IngestSource, LanguageCode, Transcript}
import com.cyrelis.linguapipe.infrastructure.config.DatabaseAdapterConfig
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import zio.test.*
import zio.test.Assertion.*

object PostgresTranscriptSinkTest extends ZIOSpecDefault:

  private val invalidConfig: DatabaseAdapterConfig.Postgres = DatabaseAdapterConfig.Postgres(
    host = "nonexistent-host",
    port = 9999,
    database = "nonexistent_db",
    user = "test_user",
    password = "test_password"
  )

  private def createTestTranscript(
    text: String = "Test transcript text",
    source: IngestSource = IngestSource.Audio,
    language: Option[String] = Some("en"),
    additionalAttributes: Map[String, String] = Map.empty
  ): Transcript =
    Transcript(
      id = UUID.randomUUID(),
      language = language.map(LanguageCode.unsafe),
      text = text,
      confidence = 0.9, // Default test confidence
      createdAt = Instant.now(),
      source = source,
      attributes = additionalAttributes
    )

  def spec = suite("PostgresTranscriptSink")(
    suite("health check scenarios")(
      test("should return Unhealthy for unreachable database") {
        val layer = QuillContext.createLayer(invalidConfig)

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          status   <- adapter.healthCheck()
        } yield assertTrue(
          status match {
            case HealthStatus.Unhealthy(serviceName, _, error, details) =>
              serviceName == "PostgreSQL" &&
              error.nonEmpty &&
              details.contains("host") &&
              details("host") == invalidConfig.host &&
              details.contains("port") &&
              details("port") == invalidConfig.port.toString &&
              details.contains("database") &&
              details("database") == invalidConfig.database
            case _ => false
          }
        )
      },
      test("should include correct service name in health status") {
        val layer = QuillContext.createLayer(invalidConfig)

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          status   <- adapter.healthCheck()
        } yield {
          val serviceName = status match {
            case HealthStatus.Healthy(name, _, _)      => name
            case HealthStatus.Unhealthy(name, _, _, _) => name
            case HealthStatus.Timeout(name, _, _)      => name
          }
          assertTrue(serviceName == "PostgreSQL")
        }
      },
      test("should include database connection details in unhealthy status") {
        val layer = QuillContext.createLayer(invalidConfig)

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          status   <- adapter.healthCheck()
        } yield {
          status match {
            case HealthStatus.Unhealthy(_, _, _, details) =>
              assertTrue(
                details.contains("host") &&
                  details.contains("port") &&
                  details.contains("database") &&
                  details("host") == invalidConfig.host &&
                  details("port") == invalidConfig.port.toString &&
                  details("database") == invalidConfig.database
              )
            case _ => assertTrue(false)
          }
        }
      }
    ),
    suite("persistTranscript scenarios")(
      test("should fail with DatabaseError for unreachable database") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript()

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      },
      test("should handle transcripts with Audio source") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript(source = IngestSource.Audio)

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      },
      test("should handle transcripts with Text source") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript(source = IngestSource.Text)

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      },
      test("should handle transcripts with Document source") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript(source = IngestSource.Document)

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      },
      test("should extract language from attributes") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript(language = Some("fr"), text = "Bonjour le monde")

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      },
      test("should handle null language when language is not determined") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript(
          text = "Text without language",
          language = None,
          additionalAttributes = Map.empty
        ).copy(
          source = IngestSource.Text,
          attributes = Map.empty
        )

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      },
      test("should serialize attributes to JSON") {
        val layer      = QuillContext.createLayer(invalidConfig)
        val transcript = createTestTranscript(
          additionalAttributes = Map(
            "provider"        -> "whisper",
            "model"           -> "whisper-1",
            "confidence"      -> "0.95",
            "processing_time" -> "1.5s"
          )
        )

        for {
          quillCtx <- ZIO.service[Quill.Postgres[SnakeCase]].provide(layer)
          adapter   = new PostgresTranscriptSink(invalidConfig, quillCtx)
          result   <- adapter.persistTranscript(transcript).exit
        } yield assert(result)(fails(isSubtype[PipelineError.DatabaseError](anything)))
      }
    )
  )
