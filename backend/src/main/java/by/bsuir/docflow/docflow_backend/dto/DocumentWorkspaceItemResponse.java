package by.bsuir.docflow.docflow_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentWorkspaceItemResponse(
        Long documentId,
        Long fileId,
        String originalName,
        String documentType,
        String status,
        String documentNumber,
        LocalDate documentDate,
        String subject,
        BigDecimal amount,
        String currency,

        String expirationStatus,
        Long daysUntilExpiration,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}