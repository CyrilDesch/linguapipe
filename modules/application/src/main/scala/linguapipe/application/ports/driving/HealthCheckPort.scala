package linguapipe.application.ports.driving

import zio.*

import linguapipe.domain.HealthStatus

trait HealthCheckPort {
  def checkAllServices(): Task[List[HealthStatus]]
}
