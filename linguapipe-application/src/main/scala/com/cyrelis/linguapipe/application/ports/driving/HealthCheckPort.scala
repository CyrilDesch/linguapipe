package com.cyrelis.linguapipe.application.ports.driving

import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

trait HealthCheckPort {
  def checkAllServices(): Task[List[HealthStatus]]
}
