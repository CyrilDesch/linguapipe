package com.example.tp.api.domain

import zio.json.JsonCodec

final case class UserToken(id: Long, email: String, token: String, expiration: Long) derives JsonCodec
