package com.cyrelis.srag.infrastructure.adapters.driven.reranker

import com.cyrelis.srag.application.types.HealthStatus
import com.cyrelis.srag.infrastructure.config.RerankerAdapterConfig
import zio.*
import zio.test.*

object TransformersRerankerAdapterTest extends ZIOSpecDefault:

  private val invalidConfig: RerankerAdapterConfig.Transformers = RerankerAdapterConfig.Transformers(
    model = "test-model",
    apiUrl = "http://nonexistent-host:9999"
  )

  def spec = suite("TransformersRerankerAdapter health checks")(
    test("should return Unhealthy for unreachable reranker service") {
      val adapter = new TransformersRerankerAdapter(invalidConfig)

      for {
        status <- adapter.healthCheck()
      } yield assertTrue(
        status match {
          case HealthStatus.Unhealthy(serviceName, _, error, details) =>
            serviceName == s"TransformersReranker(${invalidConfig.model})" &&
            error.nonEmpty &&
            details.contains("url") &&
            details("url") == invalidConfig.apiUrl &&
            details.contains("model") &&
            details("model") == invalidConfig.model
          case _ => false
        }
      )
    },
    test("should include correct service name in health status") {
      val adapter = new TransformersRerankerAdapter(invalidConfig)

      for {
        status <- adapter.healthCheck()
      } yield {
        val serviceName = status match {
          case HealthStatus.Healthy(name, _, _)      => name
          case HealthStatus.Unhealthy(name, _, _, _) => name
          case HealthStatus.Timeout(name, _, _)      => name
        }
        assertTrue(serviceName == s"TransformersReranker(${invalidConfig.model})")
      }
    },
    test("should include reranker connection details in unhealthy status") {
      val adapter = new TransformersRerankerAdapter(invalidConfig)

      for {
        status <- adapter.healthCheck()
      } yield {
        status match {
          case HealthStatus.Unhealthy(_, _, _, details) =>
            assertTrue(
              details.contains("url") &&
                details.contains("model") &&
                details("url") == invalidConfig.apiUrl &&
                details("model") == invalidConfig.model
            )
          case _ => assertTrue(false)
        }
      }
    }
  )
