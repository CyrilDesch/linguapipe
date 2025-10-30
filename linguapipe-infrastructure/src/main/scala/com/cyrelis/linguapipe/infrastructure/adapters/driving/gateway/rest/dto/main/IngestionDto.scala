package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.dto.main

import java.io.File

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.model.Part

final case class AudioIngestMultipartDto(file: Part[File], metadata: Map[String, String])

final case class TextIngestRestDto(content: String)

final case class DocumentIngestRestDto(content: String, mediaType: String)

object TextIngestRestDto {
  given Codec[TextIngestRestDto] = deriveCodec
}

object DocumentIngestRestDto {
  given Codec[DocumentIngestRestDto] = deriveCodec
}
