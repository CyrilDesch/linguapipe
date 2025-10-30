package com.cyrelis.linguapipe.application.types

import scala.concurrent.duration.FiniteDuration

final case class JobProcessingConfig(
  maxAttempts: Int,
  pollInterval: FiniteDuration,
  batchSize: Int,
  initialRetryDelay: FiniteDuration,
  maxRetryDelay: FiniteDuration,
  backoffFactor: Double
)
