package com.cyrelis.linguapipe.infrastructure.adapters.driving.gateway.rest

import zio.json.*
import com.cyrelis.linguapipe.application.types.HealthStatus

final case class AudioIngestRestDto(content: String, format: String, language: Option[String])
final case class TextIngestRestDto(content: String, language: Option[String])
final case class DocumentIngestRestDto(content: String, mediaType: String, language: Option[String])

final case class IngestionResultRestDto(
  transcriptId: String,
  segmentsEmbedded: Int,
  completedAt: String
)

final case class HealthStatusRestDto(
  status: String,
  serviceName: String,
  checkedAt: String,
  error: Option[String] = None,
  timeoutMs: Option[Long] = None,
  details: Map[String, String] = Map.empty
)

object AudioIngestRestDto {
  given JsonEncoder[AudioIngestRestDto] = DeriveJsonEncoder.gen[AudioIngestRestDto]
  given JsonDecoder[AudioIngestRestDto] = DeriveJsonDecoder.gen[AudioIngestRestDto]
}

object TextIngestRestDto {
  given JsonEncoder[TextIngestRestDto] = DeriveJsonEncoder.gen[TextIngestRestDto]
  given JsonDecoder[TextIngestRestDto] = DeriveJsonDecoder.gen[TextIngestRestDto]
}

object DocumentIngestRestDto {
  given JsonEncoder[DocumentIngestRestDto] = DeriveJsonEncoder.gen[DocumentIngestRestDto]
  given JsonDecoder[DocumentIngestRestDto] = DeriveJsonDecoder.gen[DocumentIngestRestDto]
}

object IngestionResultRestDto {
  given JsonEncoder[IngestionResultRestDto] = DeriveJsonEncoder.gen[IngestionResultRestDto]
  given JsonDecoder[IngestionResultRestDto] = DeriveJsonDecoder.gen[IngestionResultRestDto]
}

object HealthStatusRestDto {
  given JsonEncoder[HealthStatusRestDto] = DeriveJsonEncoder.gen[HealthStatusRestDto]
  given JsonDecoder[HealthStatusRestDto] = DeriveJsonDecoder.gen[HealthStatusRestDto]

  def fromApplication(healthStatus: HealthStatus): HealthStatusRestDto = healthStatus match {
    case HealthStatus.Healthy(serviceName, checkedAt, details) =>
      HealthStatusRestDto(
        status = "healthy",
        serviceName = serviceName,
        checkedAt = checkedAt.toString,
        details = details
      )
    case HealthStatus.Unhealthy(serviceName, checkedAt, error, details) =>
      HealthStatusRestDto(
        status = "unhealthy",
        serviceName = serviceName,
        checkedAt = checkedAt.toString,
        error = Some(error),
        details = details
      )
    case HealthStatus.Timeout(serviceName, checkedAt, timeoutMs) =>
      HealthStatusRestDto(
        status = "timeout",
        serviceName = serviceName,
        checkedAt = checkedAt.toString,
        timeoutMs = Some(timeoutMs)
      )
  }
}
