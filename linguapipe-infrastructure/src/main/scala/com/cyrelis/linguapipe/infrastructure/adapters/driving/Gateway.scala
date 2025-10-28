package com.cyrelis.linguapipe.infrastructure.adapters.driving

import zio.*

import com.cyrelis.linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}

trait Gateway {
  def start: ZIO[IngestPort & HealthCheckPort, Throwable, Unit]
  def description: String
}
