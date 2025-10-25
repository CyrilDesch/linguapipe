package com.example.tp.api.http

import zio.*
import sttp.tapir.server.ServerEndpoint

import com.example.tp.api.http.controllers.{HealthController, PersonController}
import com.example.tp.api.service.{JWTService, PersonService}

object HttpApi {

  def endpoints: URIO[PersonService & JWTService, List[ServerEndpoint[Any, Task]]] =
    for {
      healthController <- HealthController.makeZIO
      personController <- PersonController.makeZIO
    } yield healthController.routes ++ personController.routes
}
