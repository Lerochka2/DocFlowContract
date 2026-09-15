package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentDecisionResponse(
        Long id,
        Long documentId,
        String decision,
        String documentStatus,
        String comment,
        Long userId,
        String userFullName,
        LocalDateTime createdAt
) {
}