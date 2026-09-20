package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentArchiveResponse(
        Long documentId,
        String status,
        LocalDateTime updatedAt
) {
}