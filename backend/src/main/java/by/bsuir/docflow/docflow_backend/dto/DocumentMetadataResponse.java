package by.bsuir.docflow.docflow_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentMetadataResponse(
        Long documentId,
        String documentType,
        String status,
        String documentNumber,
        LocalDate documentDate,
        BigDecimal amount,
        String currency,
        String subject,
        LocalDate validFrom,
        LocalDate validTo,
        String referencedContractNumber,
        LocalDate referencedContractDate
) {
}