package linguapipe.application.ports.driving

import zio.*

import linguapipe.domain.Transcript

trait IngestPort {
  def executeAudio(audioContent: String, format: String, language: Option[String]): Task[Transcript]
  def executeText(textContent: String, language: Option[String]): Task[Transcript]
  def executeDocument(documentContent: String, mediaType: String, language: Option[String]): Task[Transcript]
}
