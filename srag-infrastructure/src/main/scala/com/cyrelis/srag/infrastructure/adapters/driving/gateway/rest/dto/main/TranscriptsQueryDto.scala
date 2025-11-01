package com.cyrelis.srag.infrastructure.adapters.driving.gateway.rest.dto.main

final case class TranscriptsQueryRestDto(
  sortBy: Option[String],
  metadataSort: Option[String],
  order: Option[String],
  metadata: List[String]
)
