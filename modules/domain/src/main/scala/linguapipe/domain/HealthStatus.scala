package linguapipe.domain

import java.time.Instant

import zio.json.*

enum HealthStatus:
  case Healthy(
    serviceName: String,
    checkedAt: Instant,
    details: Map[String, String] = Map.empty
  )

  case Unhealthy(
    serviceName: String,
    checkedAt: Instant,
    error: String,
    details: Map[String, String] = Map.empty
  )

  case Timeout(
    serviceName: String,
    checkedAt: Instant,
    timeoutMs: Long
  )

object HealthStatus {
  given JsonEncoder[HealthStatus] = DeriveJsonEncoder.gen[HealthStatus]
  given JsonDecoder[HealthStatus] = DeriveJsonDecoder.gen[HealthStatus]
}
