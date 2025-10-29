package com.cyrelis.linguapipe.infrastructure.resilience

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.infrastructure.config.RetryConfig
import zio.*

object RetryService {

  def applyRetry[R, A](
    effect: ZIO[R, PipelineError, A],
    config: RetryConfig
  ): ZIO[R, PipelineError, A] =
    if (config.enabled) {
      for {
        attemptRef <- Ref.make(0)
        schedule    = createSchedule(config)
        result     <- effect.tapError { error =>
                    attemptRef.updateAndGet(_ + 1).flatMap { attempt =>
                      val totalAttempts = config.maxRetries + 1
                      ZIO.logWarning(s"[Retry] Operation failed (attempt $attempt/$totalAttempts): ${error.message}")
                      if (attempt < totalAttempts) {
                        ZIO.logInfo(s"[Retry] Will retry (attempt ${attempt + 1}/$totalAttempts)")
                      } else {
                        ZIO.logError(s"[Retry] All $totalAttempts attempts exhausted. Final error: ${error.message}")
                      }
                    }
                  }
                    .retry(schedule)
      } yield result
    } else {
      effect
    }

  private def createSchedule(config: RetryConfig) = {
    val initialDelay = config.initialDelayMs.millis
    val maxDelay     = config.maxDelayMs.millis

    Schedule
      .exponential(initialDelay, config.backoffFactor)
      .modifyDelay { (_, delay) =>
        val delayMs    = delay.toMillis
        val maxDelayMs = maxDelay.toMillis
        if (delayMs > maxDelayMs) maxDelay else delay
      }
      .&&(Schedule.recurs(config.maxRetries))
  }
}
