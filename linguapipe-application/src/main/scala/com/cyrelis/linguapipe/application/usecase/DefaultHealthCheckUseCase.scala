package com.cyrelis.linguapipe.application.usecase

import com.cyrelis.linguapipe.application.ports.driven.*
import com.cyrelis.linguapipe.application.ports.driving.HealthCheckPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

final class DefaultHealthCheckUseCase(
  transcriber: TranscriberPort,
  embedder: EmbedderPort,
  dbSink: DbSinkPort,
  vectorSink: VectorSinkPort,
  blobStore: BlobStorePort
) extends HealthCheckPort {

  override def checkAllServices(): Task[List[HealthStatus]] =
    ZIO.collectAllPar(
      List(
        transcriber.healthCheck(),
        embedder.healthCheck(),
        dbSink.healthCheck(),
        vectorSink.healthCheck(),
        blobStore.healthCheck()
      )
    )

}
