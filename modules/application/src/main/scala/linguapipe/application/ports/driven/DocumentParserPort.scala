package linguapipe.application.ports.driven

import zio.*

import linguapipe.domain.IngestPayload

trait DocumentParserPort {
  def parseDocument(payload: IngestPayload.Base64Document): Task[String]
}

