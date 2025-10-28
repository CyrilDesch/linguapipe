package com.cyrelis.linguapipe.application.ports.driven

import zio.*

trait DocumentParserPort {
  def parseDocument(documentContent: String, mediaType: String): Task[String]
}
