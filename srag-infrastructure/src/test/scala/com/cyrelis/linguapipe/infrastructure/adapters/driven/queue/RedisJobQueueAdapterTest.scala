package com.cyrelis.srag.infrastructure.adapters.driven.queue

import com.cyrelis.srag.application.ports.driven.job.JobQueuePort
import com.cyrelis.srag.application.types.HealthStatus
import com.cyrelis.srag.infrastructure.config.JobQueueAdapterConfig
import zio.*
import zio.test.*

object RedisJobQueueAdapterTest extends ZIOSpecDefault:

  private val invalidConfig: JobQueueAdapterConfig.Redis = JobQueueAdapterConfig.Redis(
    host = "nonexistent-host",
    port = 9999,
    database = 0,
    password = None,
    queueKey = "test-queue",
    deadLetterKey = "test-dead-letter"
  )

  def spec = suite("RedisJobQueueAdapter health checks")(
    test("should return Unhealthy for unreachable Redis server") {
      for {
        adapter <- ZIO.service[JobQueuePort].provide(RedisJobQueueAdapter.layerFromConfig(invalidConfig))
        status  <- adapter.healthCheck()
      } yield assertTrue(
        status match {
          case HealthStatus.Unhealthy(serviceName, _, error, _) =>
            serviceName == "JobQueuePort" &&
            error.nonEmpty
          case _ => false
        }
      )
    },
    test("should include correct service name in health status") {
      for {
        adapter <- ZIO.service[JobQueuePort].provide(RedisJobQueueAdapter.layerFromConfig(invalidConfig))
        status  <- adapter.healthCheck()
      } yield {
        val serviceName = status match {
          case HealthStatus.Healthy(name, _, _)      => name
          case HealthStatus.Unhealthy(name, _, _, _) => name
          case HealthStatus.Timeout(name, _, _)      => name
        }
        assertTrue(serviceName == "JobQueuePort")
      }
    },
    test("should include error message in unhealthy status") {
      for {
        adapter <- ZIO.service[JobQueuePort].provide(RedisJobQueueAdapter.layerFromConfig(invalidConfig))
        status  <- adapter.healthCheck()
      } yield {
        status match {
          case HealthStatus.Unhealthy(_, _, error, _) =>
            assertTrue(error.nonEmpty)
          case _ => assertTrue(false)
        }
      }
    }
  )
