package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentRouteResponse(
        Long routeId,
        Long documentId,
        String documentType,
        String routingTarget,
        String routingStatus,
        String routingReason,
        LocalDateTime routedAt,
        LocalDateTime createdAt
) {
}