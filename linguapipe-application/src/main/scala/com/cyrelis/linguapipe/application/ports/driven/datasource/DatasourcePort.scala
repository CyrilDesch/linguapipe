package com.cyrelis.linguapipe.application.ports.driven.datasource

import com.cyrelis.linguapipe.application.types.HealthStatus
import zio.*

trait DatasourcePort {
  def healthCheck(): Task[HealthStatus]
}
