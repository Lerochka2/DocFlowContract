package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.ContractExpirationStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ContractExpirationService {

    private static final long EXPIRING_SOON_DAYS = 30;

    public ExpirationInfo calculate(
            Document document
    ) {

        if (
                document == null
                        || document.getDocumentType() != DocumentType.CONTRACT
        ) {

            return new ExpirationInfo(
                    null,
                    null
            );
        }

        LocalDate validTo =
                document.getValidTo();

        if (validTo == null) {

            return new ExpirationInfo(
                    ContractExpirationStatus.NOT_SPECIFIED,
                    null
            );
        }

        long daysUntilExpiration =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        validTo
                );

        if (daysUntilExpiration < 0) {

            return new ExpirationInfo(
                    ContractExpirationStatus.EXPIRED,
                    daysUntilExpiration
            );
        }

        if (
                daysUntilExpiration
                        <= EXPIRING_SOON_DAYS
        ) {

            return new ExpirationInfo(
                    ContractExpirationStatus.EXPIRING_SOON,
                    daysUntilExpiration
            );
        }

        return new ExpirationInfo(
                ContractExpirationStatus.ACTIVE,
                daysUntilExpiration
        );
    }

    public record ExpirationInfo(
            ContractExpirationStatus status,
            Long daysUntilExpiration
    ) {
    }
}