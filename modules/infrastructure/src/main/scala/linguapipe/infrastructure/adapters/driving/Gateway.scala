package linguapipe.infrastructure.adapters.driving

import zio.*

import linguapipe.application.ports.driving.{HealthCheckPort, IngestPort}

trait Gateway {
  def start: ZIO[IngestPort & HealthCheckPort, Throwable, Unit]
  def description: String
}
