package com.cyrelis.linguapipe.application.ports.driving

import zio.*

import com.cyrelis.linguapipe.application.types.HealthStatus

trait HealthCheckPort {
  def checkAllServices(): Task[List[HealthStatus]]
}
