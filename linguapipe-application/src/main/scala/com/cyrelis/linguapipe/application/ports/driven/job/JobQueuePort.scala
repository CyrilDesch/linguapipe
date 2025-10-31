package com.cyrelis.linguapipe.application.ports.driven.job

import java.util.UUID

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

trait JobQueuePort {
  def enqueue(jobId: UUID): ZIO[Any, PipelineError, Unit]
  def dequeueBatch(max: Int): ZIO[Any, PipelineError, List[UUID]]
  def ack(jobId: UUID): ZIO[Any, PipelineError, Unit]
  def retry(jobId: UUID, attempt: Int, delay: zio.Duration): ZIO[Any, PipelineError, Unit]
  def deadLetter(jobId: UUID, reason: String): ZIO[Any, PipelineError, Unit]
  def healthCheck(): Task[HealthStatus]
}
