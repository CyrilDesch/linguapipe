package linguapipe.application.ports.driving

import zio.*

import linguapipe.domain.*

trait IngestPort {
  def execute(command: IngestCommand): Task[IngestionResult]
}
