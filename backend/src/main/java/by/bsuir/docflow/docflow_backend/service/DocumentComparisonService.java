package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentComparisonResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DocumentComparisonService {

    private static final String STATUS_SAME = "SAME";
    private static final String STATUS_CHANGED = "CHANGED";
    private static final String STATUS_NOT_SPECIFIED = "NOT_SPECIFIED";

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentAccessService documentAccessService;

    public DocumentComparisonService(
            DocumentRepository documentRepository,
            DocumentLinkRepository documentLinkRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRepository = documentRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.documentAccessService = documentAccessService;
    }

    @Transactional(readOnly = true)
    public DocumentComparisonResponse compare(
            Long additionalDocumentId
    ) {

        Document additionalDocument =
                documentRepository
                        .findById(additionalDocumentId)
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + additionalDocumentId
                                                + " не найден"
                                )
                        );

        documentAccessService.requireAccess(
                additionalDocument
        );

        validateAdditionalDocument(
                additionalDocument
        );

        DocumentLink link =
                documentLinkRepository
                        .findByChildDocumentId(
                                additionalDocumentId
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Дополнительное соглашение не связано "
                                                + "с основным договором"
                                )
                        );

        Document contract =
                link.getContractDocument();

        if (contract == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Связанный основной договор отсутствует"
            );
        }

        documentAccessService.requireAccess(
                contract
        );

        if (contract.getDocumentType()
                != DocumentType.CONTRACT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Связанный документ не является основным договором"
            );
        }

        List<DocumentComparisonResponse.FieldComparison>
                comparisons =
                new ArrayList<>();

        comparisons.add(
                compareAmount(
                        contract.getAmount(),
                        additionalDocument.getAmount()
                )
        );

        comparisons.add(
                compareString(
                        "currency",
                        "Валюта",
                        contract.getCurrency(),
                        additionalDocument.getCurrency()
                )
        );

        comparisons.add(
                compareString(
                        "subject",
                        "Предмет договора",
                        contract.getSubject(),
                        additionalDocument.getSubject()
                )
        );

        comparisons.add(
                compareDate(
                        "validFrom",
                        "Дата начала действия",
                        contract.getValidFrom(),
                        additionalDocument.getValidFrom()
                )
        );

        comparisons.add(
                compareDate(
                        "validTo",
                        "Дата окончания действия",
                        contract.getValidTo(),
                        additionalDocument.getValidTo()
                )
        );

        int changedFieldsCount =
                (int) comparisons
                        .stream()
                        .filter(
                                comparison ->
                                        STATUS_CHANGED.equals(
                                                comparison.status()
                                        )
                        )
                        .count();

        Boolean referencedContractMatches =
                determineReferencedContractMatch(
                        contract,
                        additionalDocument
                );

        return new DocumentComparisonResponse(

                additionalDocument.getId(),
                additionalDocument
                        .getDocumentType()
                        .name(),
                additionalDocument.getDocumentNumber(),
                additionalDocument.getDocumentDate(),

                contract.getId(),
                contract.getDocumentNumber(),
                contract.getDocumentDate(),

                referencedContractMatches,

                changedFieldsCount,

                comparisons
        );
    }

    private void validateAdditionalDocument(
            Document document
    ) {

        DocumentType type =
                document.getDocumentType();

        if (
                type != DocumentType.AMENDMENT_CHANGE
                        && type
                        != DocumentType.AMENDMENT_PROLONGATION
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Сравнение с основным договором доступно "
                            + "только для дополнительного соглашения"
            );
        }
    }

    private DocumentComparisonResponse.FieldComparison
    compareAmount(
            BigDecimal contractValue,
            BigDecimal additionalValue
    ) {

        String status;

        if (additionalValue == null) {

            status = STATUS_NOT_SPECIFIED;

        } else if (
                contractValue != null
                        && contractValue.compareTo(
                        additionalValue
                ) == 0
        ) {

            status = STATUS_SAME;

        } else {

            status = STATUS_CHANGED;
        }

        return new DocumentComparisonResponse.FieldComparison(
                "amount",
                "Сумма",
                toStringValue(contractValue),
                toStringValue(additionalValue),
                status
        );
    }

    private DocumentComparisonResponse.FieldComparison
    compareString(
            String field,
            String label,
            String contractValue,
            String additionalValue
    ) {

        String status;

        if (isBlank(additionalValue)) {

            status = STATUS_NOT_SPECIFIED;

        } else if (
                normalizedEquals(
                        contractValue,
                        additionalValue
                )
        ) {

            status = STATUS_SAME;

        } else {

            status = STATUS_CHANGED;
        }

        return new DocumentComparisonResponse.FieldComparison(
                field,
                label,
                contractValue,
                additionalValue,
                status
        );
    }

    private DocumentComparisonResponse.FieldComparison
    compareDate(
            String field,
            String label,
            LocalDate contractValue,
            LocalDate additionalValue
    ) {

        String status;

        if (additionalValue == null) {

            status = STATUS_NOT_SPECIFIED;

        } else if (
                Objects.equals(
                        contractValue,
                        additionalValue
                )
        ) {

            status = STATUS_SAME;

        } else {

            status = STATUS_CHANGED;
        }

        return new DocumentComparisonResponse.FieldComparison(
                field,
                label,
                toStringValue(contractValue),
                toStringValue(additionalValue),
                status
        );
    }

    private Boolean determineReferencedContractMatch(
            Document contract,
            Document additionalDocument
    ) {

        String referencedNumber =
                additionalDocument
                        .getReferencedContractNumber();

        LocalDate referencedDate =
                additionalDocument
                        .getReferencedContractDate();

        if (
                isBlank(referencedNumber)
                        && referencedDate == null
        ) {
            return null;
        }

        boolean numberMatches = true;
        boolean dateMatches = true;

        if (!isBlank(referencedNumber)) {

            numberMatches =
                    normalizedEquals(
                            contract.getDocumentNumber(),
                            referencedNumber
                    );
        }

        if (referencedDate != null) {

            dateMatches =
                    Objects.equals(
                            contract.getDocumentDate(),
                            referencedDate
                    );
        }

        return numberMatches
                && dateMatches;
    }

    private boolean normalizedEquals(
            String first,
            String second
    ) {

        if (first == null || second == null) {
            return first == null
                    && second == null;
        }

        return first
                .trim()
                .equalsIgnoreCase(
                        second.trim()
                );
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }

    private String toStringValue(
            Object value
    ) {

        return value == null
                ? null
                : value.toString();
    }
}