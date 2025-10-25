package com.example.tp.api.http.controllers

import zio.*

import sttp.tapir.ztapir.*
import sttp.tapir.ztapir.ZServerEndpoint

import com.example.tp.api.domain.*
import com.example.tp.api.http.endpoints.PersonEndpoint
import com.example.tp.api.service.{JWTService, PersonService}

final class PersonController private (personService: PersonService, jwtService: JWTService) {

  private type ApiEndpoint = ZServerEndpoint[Any, Any]

  private val authenticate: String => Task[UserID] = jwtService.verifyToken

  private val create: ApiEndpoint =
    PersonEndpoint.create.zServerLogic(personService.register)

  private val login: ApiEndpoint = PersonEndpoint.login.zServerLogic { credentials =>
    for {
      user  <- personService.login(credentials.login, credentials.password)
      token <- jwtService.createToken(user)
    } yield token
  }

  private val profile: ApiEndpoint =
    PersonEndpoint.profile
      .zServerSecurityLogic(authenticate)
      .serverLogic(userId => withPet => personService.getProfile(userId, withPet))

  private val listPets: ApiEndpoint =
    PersonEndpoint.listPets.zServerLogic(personService.listPets)

  val routes: List[ApiEndpoint] = List(create, login, profile, listPets)
}

object PersonController {
  def makeZIO: URIO[PersonService & JWTService, PersonController] =
    for {
      jwtService    <- ZIO.service[JWTService]
      personService <- ZIO.service[PersonService]
    } yield new PersonController(personService, jwtService)
}
