package com.cyrelis.linguapipe.infrastructure.adapters.driving

import zio.*

trait Gateway {
  def start: ZIO[Any, Throwable, Unit]
  def description: String
}
