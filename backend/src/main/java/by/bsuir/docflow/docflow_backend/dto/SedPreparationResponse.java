package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDate;

public record SedPreparationResponse(
        Long documentId,
        String documentType,
        String status,
        String documentNumber,
        LocalDate documentDate,
        String subject,
        String routingTarget
) {
}