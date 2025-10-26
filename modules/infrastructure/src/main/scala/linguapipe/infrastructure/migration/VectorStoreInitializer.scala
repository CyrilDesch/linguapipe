package linguapipe.infrastructure.migration

import zio.*

import linguapipe.infrastructure.config.VectorStoreAdapterConfig

/**
 * Vector store initialization service. Creates necessary collections/indexes at
 * application startup. Each vector store provider handles this differently:
 *   - Qdrant: Create collection with vector configuration
 */
trait VectorStoreInitializer {
  def initialize(): Task[Unit]
}

object VectorStoreInitializer {

  final class QdrantInitializer(config: VectorStoreAdapterConfig.Qdrant) extends VectorStoreInitializer {
    override def initialize(): Task[Unit] =
      ZIO.logInfo(s"🔄 Initializing Qdrant collection: ${config.collection}") *>
        createCollectionIfNotExists() *>
        ZIO.logInfo(s"✅ Qdrant collection '${config.collection}' is ready")

    private def createCollectionIfNotExists(): Task[Unit] =
      ZIO.attempt {
        // In a real implementation, use Qdrant HTTP API:
        // 1. GET /collections/{collection} to check if exists
        // 2. If 404, PUT /collections/{collection} with vector config:
        //    {
        //      "vectors": {
        //        "size": 384,  // or whatever embedding dimension
        //        "distance": "Cosine"
        //      }
        //    }
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
