package com.cyrelis.srag.application.ports.driven.parser

import com.cyrelis.srag.application.errors.PipelineError
import zio.*

trait DocumentParserPort {
  def parseDocument(documentContent: String, mediaType: String): ZIO[Any, PipelineError, String]
}
