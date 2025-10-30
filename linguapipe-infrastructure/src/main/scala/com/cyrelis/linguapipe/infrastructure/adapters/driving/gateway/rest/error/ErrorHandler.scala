package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest.error

import com.cyrelis.linguapipe.application.errors.PipelineError

object ErrorHandler {
  def errorToString(error: Throwable | PipelineError): String = error match {
    case pipelineError: PipelineError => pipelineError.message
    case throwable: Throwable         => throwable.getMessage
  }
}
