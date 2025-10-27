package linguapipe.infrastructure.runtime

import zio.*

import linguapipe.application.ports.driving.HealthCheckPort
import linguapipe.domain.HealthStatus

object LinguaPipeRuntime {
  def make: ZIO[HealthCheckPort, Throwable, Unit] =
    for {
      healthCheckPort <- ZIO.service[HealthCheckPort]
      _               <- ZIO.logInfo("Running health checks...")
      healthResults   <- healthCheckPort.checkAllServices().orElse(ZIO.succeed(List.empty))
      _               <- logHealthResults(healthResults)
      _               <- ZIO.logInfo("All systems operational")
    } yield ()

  private def logHealthResults(results: List[HealthStatus]): ZIO[Any, Throwable, Unit] =
    ZIO
      .foreach(results) { status =>
        status match {
          case HealthStatus.Healthy(serviceName, _, _) =>
            ZIO.logInfo(s"✓ $serviceName")
          case HealthStatus.Unhealthy(serviceName, _, error, _) =>
            ZIO.logWarning(s"✗ $serviceName: $error")
          case HealthStatus.Timeout(serviceName, _, timeoutMs) =>
            ZIO.logWarning(s"✗ $serviceName: timeout after ${timeoutMs}ms")
        }
      }
      .unit
}
