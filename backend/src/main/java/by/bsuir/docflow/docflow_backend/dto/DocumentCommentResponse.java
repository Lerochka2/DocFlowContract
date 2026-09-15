package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentCommentResponse(
        Long id,
        Long documentId,
        Long userId,
        String userFullName,
        String comment,
        LocalDateTime createdAt
) {
}