package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentAuditEventResponse(
        Long id,
        Long documentId,
        String eventType,
        String actorType,
        Long userId,
        String actorName,
        String description,
        String details,
        LocalDateTime createdAt
) {
}