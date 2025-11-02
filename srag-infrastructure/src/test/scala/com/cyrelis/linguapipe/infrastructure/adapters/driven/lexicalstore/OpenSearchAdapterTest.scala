package com.cyrelis.srag.infrastructure.adapters.driven.lexicalstore

import com.cyrelis.srag.application.types.HealthStatus
import com.cyrelis.srag.infrastructure.config.LexicalStoreAdapterConfig
import zio.*
import zio.test.*

object OpenSearchAdapterTest extends ZIOSpecDefault:

  private val invalidConfig: LexicalStoreAdapterConfig.OpenSearch = LexicalStoreAdapterConfig.OpenSearch(
    url = "http://nonexistent-host:9200",
    index = "test-index",
    username = None,
    password = None
  )

  def spec = suite("OpenSearchAdapter health checks")(
    test("should return Unhealthy for unreachable OpenSearch server") {
      val adapter = new OpenSearchAdapter(invalidConfig)

      for {
        status <- adapter.healthCheck()
      } yield assertTrue(
        status match {
          case HealthStatus.Unhealthy(serviceName, _, error, details) =>
            serviceName == s"OpenSearch(${invalidConfig.index})" &&
            error.nonEmpty &&
            details.contains("url") &&
            details("url") == invalidConfig.url &&
            details.contains("index") &&
            details("index") == invalidConfig.index
          case _ => false
        }
      )
    },
    test("should include correct service name in health status") {
      val adapter = new OpenSearchAdapter(invalidConfig)

      for {
        status <- adapter.healthCheck()
      } yield {
        val serviceName = status match {
          case HealthStatus.Healthy(name, _, _)      => name
          case HealthStatus.Unhealthy(name, _, _, _) => name
          case HealthStatus.Timeout(name, _, _)      => name
        }
        assertTrue(serviceName == s"OpenSearch(${invalidConfig.index})")
      }
    },
    test("should include OpenSearch connection details in unhealthy status") {
      val adapter = new OpenSearchAdapter(invalidConfig)

      for {
        status <- adapter.healthCheck()
      } yield {
        status match {
          case HealthStatus.Unhealthy(_, _, _, details) =>
            assertTrue(
              details.contains("url") &&
                details.contains("index") &&
                details("url") == invalidConfig.url &&
                details("index") == invalidConfig.index
            )
          case _ => assertTrue(false)
        }
      }
    }
  )
