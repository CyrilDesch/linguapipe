package com.cyrelis.linguapipe.application.types

final case class RerankerResult(
  candidate: RerankerCandidate,
  score: Double
)
