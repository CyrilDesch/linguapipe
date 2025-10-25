package com.example.tp.api.http.controllers

import zio.*

import sttp.tapir.ztapir.*
import sttp.tapir.ztapir.ZServerEndpoint

import com.example.tp.api.http.endpoints.HealthEndpoint

final class HealthController private {

  private type ApiEndpoint = ZServerEndpoint[Any, Any]

  private val health: ApiEndpoint =
    HealthEndpoint.healthEndpoint.zServerLogic(_ => ZIO.succeed("OK"))

  val routes: List[ApiEndpoint] = List(health)
}

object HealthController {
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
}
