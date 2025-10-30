package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.main

import com.cyrelis.linguapipe.application.types.HealthStatus
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class HealthStatusRestDto(
  status: String,
  serviceName: String,
  checkedAt: String,
  error: Option[String] = None,
  timeoutMs: Option[Long] = None,
  details: Map[String, String] = Map.empty
)

object HealthStatusRestDto {
  given Codec[HealthStatusRestDto] = deriveCodec

  def fromDomain(healthStatus: HealthStatus): HealthStatusRestDto = healthStatus match {
    case HealthStatus.Healthy(serviceName, checkedAt, details) =>
      HealthStatusRestDto(
        status = "healthy",
        serviceName = serviceName,
        checkedAt = checkedAt.toString,
        details = details
      )
    case HealthStatus.Unhealthy(serviceName, checkedAt, error, details) =>
      HealthStatusRestDto(
        status = "unhealthy",
        serviceName = serviceName,
        checkedAt = checkedAt.toString,
        error = Some(error),
        details = details
      )
    case HealthStatus.Timeout(serviceName, checkedAt, timeoutMs) =>
      HealthStatusRestDto(
        status = "timeout",
        serviceName = serviceName,
        checkedAt = checkedAt.toString,
        timeoutMs = Some(timeoutMs)
      )
  }
}
