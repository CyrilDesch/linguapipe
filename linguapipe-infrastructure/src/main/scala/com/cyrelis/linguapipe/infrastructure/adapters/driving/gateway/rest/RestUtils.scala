package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import java.io.File

import sttp.model.Part

object RestUtils {
  def extractFormat(part: Part[File]): String =
    part.fileName.flatMap { name =>
      val lastDot = name.lastIndexOf('.')
      if (lastDot > 0 && lastDot < name.length - 1) Some(name.substring(lastDot + 1))
      else None
    }
      .getOrElse("wav")
}
