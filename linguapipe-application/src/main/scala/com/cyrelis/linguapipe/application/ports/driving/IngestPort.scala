package com.cyrelis.linguapipe.application.ports.driving

import com.cyrelis.linguapipe.domain.Transcript
import zio.*

trait IngestPort {
  def executeAudio(audioContent: Array[Byte], format: String): Task[Transcript]
  def executeText(textContent: String): Task[Transcript]
  def executeDocument(documentContent: String, mediaType: String): Task[Transcript]
}
