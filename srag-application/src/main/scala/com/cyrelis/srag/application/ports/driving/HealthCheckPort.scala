package com.cyrelis.srag.application.ports.driving

import com.cyrelis.srag.application.types.HealthStatus
import zio.*

trait HealthCheckPort {
  def checkAllServices(): Task[List[HealthStatus]]
}
