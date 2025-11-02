package com.cyrelis.srag.application.types

final case class RerankerResult(
  candidate: RerankerCandidate,
  score: Double
)
