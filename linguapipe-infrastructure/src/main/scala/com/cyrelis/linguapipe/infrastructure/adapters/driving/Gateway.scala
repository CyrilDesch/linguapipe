package com.cyrelis.linguapipe.infrastructure.adapters.driving

import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}
import com.cyrelis.linguapipe.infrastructure.config.RuntimeConfig
import zio.*

trait Gateway {
  def start: ZIO[IngestPort & HealthCheckPort & RuntimeConfig, Throwable, Unit]
  def description: String
}
