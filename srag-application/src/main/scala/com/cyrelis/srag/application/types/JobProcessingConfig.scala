package com.cyrelis.srag.application.types

import scala.concurrent.duration.FiniteDuration

final case class JobProcessingConfig(
  maxAttempts: Int,
  pollInterval: FiniteDuration,
  batchSize: Int,
  initialRetryDelay: FiniteDuration,
  maxRetryDelay: FiniteDuration,
  backoffFactor: Double,
  maxConcurrentJobs: Int = 1
)
