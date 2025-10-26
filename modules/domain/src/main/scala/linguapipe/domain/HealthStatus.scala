package linguapipe.domain

import java.time.Instant

/** Health status of an adapter or service */
enum HealthStatus:
  /** Service is healthy and operational */
  case Healthy(
    serviceName: String,
    checkedAt: Instant,
    details: Map[String, String] = Map.empty
  )

  /** Service is unhealthy or unreachable */
  case Unhealthy(
    serviceName: String,
    checkedAt: Instant,
    error: String,
    details: Map[String, String] = Map.empty
  )

  /** Service health check timed out */
  case Timeout(
    serviceName: String,
    checkedAt: Instant,
    timeoutMs: Long
  )
