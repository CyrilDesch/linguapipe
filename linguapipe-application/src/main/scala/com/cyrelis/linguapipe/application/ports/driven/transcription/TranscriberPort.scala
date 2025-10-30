package com.cyrelis.linguapipe.application.ports.driven.transcription

import com.cyrelis.linguapipe.application.errors.PipelineError
import com.cyrelis.linguapipe.application.types.HealthStatus
import com.cyrelis.linguapipe.domain.transcript.Transcript
import zio.*

trait TranscriberPort {
  def transcribe(audioContent: Array[Byte], format: String): ZIO[Any, PipelineError, Transcript]
  def healthCheck(): Task[HealthStatus]
}
