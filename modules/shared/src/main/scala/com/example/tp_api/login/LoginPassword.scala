package com.example.tp.api.login

import sttp.tapir.Schema
import com.example.tp.api.domain.Password

final case class LoginPassword(login: String, password: Password) derives zio.json.JsonCodec, Schema:
  def isIncomplete: Boolean = login.isBlank || password.isBlank
