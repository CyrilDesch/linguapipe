package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.test

import java.io.File

import sttp.model.Part

final case class TestBlobStoreRestDto(file: Part[File])
