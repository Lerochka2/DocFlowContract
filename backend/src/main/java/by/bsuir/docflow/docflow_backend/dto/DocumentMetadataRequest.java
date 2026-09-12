package by.bsuir.docflow.docflow_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentMetadataRequest(
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