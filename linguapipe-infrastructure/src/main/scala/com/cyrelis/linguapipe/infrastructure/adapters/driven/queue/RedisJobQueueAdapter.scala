package com.cyrelis.linguapipe.infrastructure.adapters.driven.queue

import java.util.UUID

import scala.jdk.CollectionConverters.*

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.ports.driven.job.JobQueuePort
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.infrastructure.config.JobQueueAdapterConfig
import redis.clients.jedis.{DefaultJedisClientConfig, HostAndPort, JedisPool, JedisPoolConfig}
import zio.*

final class RedisJobQueueAdapter(
  pool: JedisPool,
  queueKey: String,
  deadLetterKey: String
) extends JobQueuePort {

  override def enqueue(jobId: UUID): ZIO[Any, PipelineError, Unit] =
    ZIO.attemptBlocking {
      val jedis = pool.getResource
      try jedis.rpush(queueKey, jobId.toString)
      finally jedis.close()
    }.unit.mapError(e => PipelineError.QueueError("Redis enqueue failed", Some(e)))

  override def dequeueBatch(max: Int): ZIO[Any, PipelineError, List[UUID]] =
    ZIO.attemptBlocking {
      val jedis = pool.getResource
      try {
        val popped = jedis.lpop(queueKey, max)
        if (popped == null) List.empty
        else popped.asScala.iterator.map(UUID.fromString).toList
      } finally jedis.close()
    }.mapError(e => PipelineError.QueueError("Redis dequeue failed", Some(e)))

  override def ack(jobId: UUID): ZIO[Any, PipelineError, Unit] = ZIO.unit

  override def retry(jobId: UUID, attempt: Int, delay: Duration): ZIO[Any, PipelineError, Unit] =
    ZIO.sleep(delay) *>
      ZIO.attemptBlocking {
        val jedis = pool.getResource
        try jedis.rpush(queueKey, jobId.toString)
        finally jedis.close()
      }.unit.mapError(e => PipelineError.QueueError("Redis retry enqueue failed", Some(e)))

  override def deadLetter(jobId: UUID, reason: String): ZIO[Any, PipelineError, Unit] =
    ZIO.attemptBlocking {
      val jedis = pool.getResource
      try jedis.rpush(deadLetterKey, s"${jobId.toString}:$reason")
      finally jedis.close()
    }.unit.mapError(e => PipelineError.QueueError("Redis deadLetter failed", Some(e)))

  override def healthCheck(): Task[HealthStatus] =
    ZIO.attemptBlocking {
      val jedis = pool.getResource
      try {
        val pong = jedis.ping()
        if (pong != null && pong.equalsIgnoreCase("PONG"))
          HealthStatus.Healthy("JobQueuePort", java.time.Instant.now(), Map("ping" -> "PONG"))
        else
          HealthStatus.Unhealthy(
            "JobQueuePort",
            java.time.Instant.now(),
            s"Unexpected PING response: ${Option(pong).getOrElse("null")}"
          )
      } finally jedis.close()
    }.catchAll(e => ZIO.succeed(HealthStatus.Unhealthy("JobQueuePort", java.time.Instant.now(), e.getMessage)))
}

object RedisJobQueueAdapter {

  def layerFromConfig(config: JobQueueAdapterConfig.Redis): ZLayer[Any, Throwable, JobQueuePort] =
    ZLayer.scoped {
      for {
        pool <- ZIO.acquireRelease(
                  ZIO.attemptBlocking {
                    val clientCfg = DefaultJedisClientConfig
                      .builder()
                      .password(config.password.orNull)
                      .database(config.database)
                      .build()

                    val poolCfg = new JedisPoolConfig()

                    new JedisPool(poolCfg, new HostAndPort(config.host, config.port), clientCfg)
                  }
                )((pool: JedisPool) => ZIO.attempt(pool.close()).ignore)
      } yield new RedisJobQueueAdapter(
        pool = pool,
        queueKey = config.queueKey,
        deadLetterKey = config.deadLetterKey
      )
    }
}
