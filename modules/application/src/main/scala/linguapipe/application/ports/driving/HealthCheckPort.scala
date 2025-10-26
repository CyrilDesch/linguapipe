package linguapipe.application.ports.driving

import zio.*

import linguapipe.domain.HealthStatus

/**
 * Port for health check operations exposed to external adapters (HTTP, gRPC,
 * etc.)
 */
trait HealthCheckPort {
  def checkAllServices(): Task[List[HealthStatus]]
}
