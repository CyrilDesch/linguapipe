package linguapipe.application.ports.driving

import zio.*

import linguapipe.domain.*

/**
 * Port driving exposé par l'application pour les adaptateurs externes (HTTP,
 * gRPC, etc.).
 */
trait IngestPort {
  def execute(command: IngestCommand): Task[IngestionResult]
}
