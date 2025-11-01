package com.cyrelis.srag.application.ports.driven.datasource

import com.cyrelis.srag.application.types.HealthStatus
import zio.*

trait DatasourcePort {
  def healthCheck(): Task[HealthStatus]
}
