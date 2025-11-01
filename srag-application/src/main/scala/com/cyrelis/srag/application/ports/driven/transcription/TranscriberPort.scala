package com.cyrelis.srag.application.ports.driven.transcription

import com.cyrelis.srag.application.errors.PipelineError
import com.cyrelis.srag.application.types.HealthStatus
import com.cyrelis.srag.domain.transcript.Transcript
import zio.*

trait TranscriberPort {
  def transcribe(
    audioContent: Array[Byte],
    mediaContentType: String,
    mediaFilename: String
  ): ZIO[Any, PipelineError, Transcript]
  def healthCheck(): Task[HealthStatus]
}
