package by.bsuir.docflow.docflow_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentSummaryResponse(
        Long documentId,
        Long fileId,
        String originalName,
        String documentType,
        String status,

        String documentNumber,
        LocalDate documentDate,
        BigDecimal amount,
        String currency,
        String subject,
        LocalDate validFrom,
        LocalDate validTo,

        String expirationStatus,
        Long daysUntilExpiration,

        AiSummary ai,
        RoutingSummary routing,
        LinkSummary link,
        SedSummary sed,

        int linkedDocumentsCount
) {

    public record AiSummary(
            Long resultId,
            String predictedType,
            BigDecimal confidence,
            String processingStatus
    ) {
    }

    public record RoutingSummary(
            Long routeId,
            String routingTarget,
            String routingStatus,
            String routingReason,
            LocalDateTime routedAt
    ) {
    }

    public record LinkSummary(
            Long linkId,
            Long contractDocumentId,
            String contractDocumentNumber,
            LocalDate contractDocumentDate,
            LocalDateTime linkedAt
    ) {
    }

    public record SedSummary(
            Long dispatchId,
            String dispatchStatus,
            String externalDocumentId,
            String responseMessage,
            LocalDateTime sentAt
    ) {
    }
}