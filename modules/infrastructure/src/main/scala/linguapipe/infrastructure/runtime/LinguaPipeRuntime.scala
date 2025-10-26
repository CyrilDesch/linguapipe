package linguapipe.infrastructure.runtime

import zio.*

import linguapipe.application.ports.driving.HealthCheckPort
import linguapipe.domain.HealthStatus

/** Entry point assembling runtime layers. */
object LinguaPipeRuntime {
  def make(healthCheckPort: HealthCheckPort): UIO[Unit] =
    for {
      _             <- ZIO.logInfo("🔍 Running health checks...")
      healthResults <- healthCheckPort.checkAllServices().orElse(ZIO.succeed(List.empty))
      _             <- logHealthResults(healthResults)
      _             <- ZIO.logInfo("✅ All systems operational")
    } yield ()

  private def logHealthResults(results: List[HealthStatus]): UIO[Unit] =
    ZIO
      .foreach(results) { status =>
        status match {
          case HealthStatus.Healthy(serviceName, _, details) =>
            val detailsStr = if (details.nonEmpty) s" (${details.mkString(", ")})" else ""
            ZIO.logInfo(s"  ✅ $serviceName$detailsStr")
          case HealthStatus.Unhealthy(serviceName, _, error, details) =>
            val detailsStr = if (details.nonEmpty) s" (${details.mkString(", ")})" else ""
            ZIO.logWarning(s"  ❌ $serviceName: $error$detailsStr")
          case HealthStatus.Timeout(serviceName, _, timeoutMs) =>
            ZIO.logWarning(s"  ⏰ $serviceName: timeout after ${timeoutMs}ms")
        }
      }
      .unit
}
