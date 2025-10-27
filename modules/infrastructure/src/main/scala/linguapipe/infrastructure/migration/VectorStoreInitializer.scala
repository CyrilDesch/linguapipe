package linguapipe.infrastructure.migration

import zio.*

import linguapipe.infrastructure.config.VectorStoreAdapterConfig

trait VectorStoreInitializer {
  def initialize(): Task[Unit]
}

object VectorStoreInitializer {

  final class QdrantInitializer(config: VectorStoreAdapterConfig.Qdrant) extends VectorStoreInitializer {
    override def initialize(): Task[Unit] =
      ZIO.logInfo(s"Initializing Qdrant collection: ${config.collection}") *>
        createCollectionIfNotExists() *>
        ZIO.logInfo(s"Qdrant collection '${config.collection}' is ready")

    private def createCollectionIfNotExists(): Task[Unit] =
      ZIO.attempt {
        println(s"[Qdrant Init] Would create collection '${config.collection}' at ${config.url}")
      }
  }

  def layer: ZLayer[VectorStoreAdapterConfig, Nothing, VectorStoreInitializer] =
    ZLayer.fromFunction { (config: VectorStoreAdapterConfig) =>
      config match {
        case cfg: VectorStoreAdapterConfig.Qdrant => new QdrantInitializer(cfg)
      }
    }
}
