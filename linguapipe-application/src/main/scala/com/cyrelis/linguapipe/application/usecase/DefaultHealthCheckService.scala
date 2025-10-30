package com.cyrelis.linguapipe.application.service

import com.cyrelis.linguapipe.application.ports.driven.datasource.DatasourcePort
import com.cyrelis.linguapipe.application.ports.driven.embedding.EmbedderPort
import com.cyrelis.linguapipe.application.ports.driven.job.JobQueuePort
import com.cyrelis.linguapipe.application.ports.driven.storage.{BlobStorePort, VectorStorePort}
import com.cyrelis.linguapipe.application.ports.driven.transcription.TranscriberPort
import com.cyrelis.linguapipe.application.ports.driving.HealthCheckPort
import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

final class DefaultHealthCheckService(
  transcriber: TranscriberPort,
  embedder: EmbedderPort,
  datasource: DatasourcePort,
  vectorSink: VectorStorePort,
  blobStore: BlobStorePort,
  jobQueue: JobQueuePort
) extends HealthCheckPort {

  override def checkAllServices(): Task[List[HealthStatus]] =
    ZIO.collectAllPar(
      List(
        transcriber.healthCheck(),
        embedder.healthCheck(),
        datasource.healthCheck(),
        vectorSink.healthCheck(),
        blobStore.healthCheck(),
        jobQueue.healthCheck()
      )
    )

}
