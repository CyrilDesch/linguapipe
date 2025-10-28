package com.cyrelis.linguapipe.application.ports.driving

import zio.*

import com.cyrelis.linguapipe.domain.HealthStatus

trait HealthCheckPort {
  def checkAllServices(): Task[List[HealthStatus]]
}
