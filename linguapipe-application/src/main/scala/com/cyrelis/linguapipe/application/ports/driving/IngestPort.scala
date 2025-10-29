package com.cyrelis.linguapipe.application.ports.driving

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.domain.Transcript
import zio.*

trait IngestPort {
  def executeAudio(audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Transcript]
  def executeText(textContent: String): ZIO[Any, PipelineError, Transcript]
  def executeDocument(documentContent: String, mediaType: String): ZIO[Any, PipelineError, Transcript]
}
