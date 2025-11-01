package com.cyrelis.srag.infrastructure.adapters.driving.gateway.grpc

import com.cyrelis.srag.application.ports.driving.IngestPort

/** gRPC adapter exposing ingestion via driving port. */
final class IngestGrpcGateway(ingestPort: IngestPort) {
  def description: String =
    s"gRPC gateway bound to ingest port: ${ingestPort.getClass.getSimpleName}"
}
