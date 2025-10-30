package com.cyrelis.linguapipe.application.ports.driven.parser

import com.cyrelis.linguapipe.application.errors.PipelineError
import zio.*

trait DocumentParserPort {
  def parseDocument(documentContent: String, mediaType: String): ZIO[Any, PipelineError, String]
}
