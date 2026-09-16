package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentMetadataRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentMetadataResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DocumentMetadataService {

    private final DocumentRepository documentRepository;
    private final SedDispatchRepository sedDispatchRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentAuditService documentAuditService;

    public DocumentMetadataService(
            DocumentRepository documentRepository,
            SedDispatchRepository sedDispatchRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService
    ) {
        this.documentRepository =
                documentRepository;

        this.sedDispatchRepository =
                sedDispatchRepository;

        this.documentAccessService =
                documentAccessService;

        this.documentAuditService =
                documentAuditService;
    }

    @Transactional
    public DocumentMetadataResponse updateMetadata(
            Long documentId,
            DocumentMetadataRequest request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо передать метаданные документа"
            );
        }

        Document document =
                getAccessibleDocument(
                        documentId
                );

        validateEditingAllowed(
                document
        );

        validate(
                request
        );

        String newDocumentNumber =
                normalizeText(
                        request.documentNumber()
                );

        String newCurrency =
                normalizeCurrency(
                        request.currency()
                );

        String newSubject =
                normalizeText(
                        request.subject()
                );

        String newReferencedContractNumber =
                normalizeText(
                        request.referencedContractNumber()
                );

        List<String> changes =
                new ArrayList<>();

        addChange(
                changes,
                "Номер документа",
                document.getDocumentNumber(),
                newDocumentNumber
        );

        addChange(
                changes,
                "Дата документа",
                document.getDocumentDate(),
                request.documentDate()
        );

        addAmountChange(
                changes,
                document.getAmount(),
                request.amount()
        );

        addChange(
                changes,
                "Валюта",
                document.getCurrency(),
                newCurrency
        );

        addChange(
                changes,
                "Предмет документа",
                document.getSubject(),
                newSubject
        );

        addChange(
                changes,
                "Дата начала действия",
                document.getValidFrom(),
                request.validFrom()
        );

        addChange(
                changes,
                "Дата окончания действия",
                document.getValidTo(),
                request.validTo()
        );

        addChange(
                changes,
                "Номер основного договора",
                document.getReferencedContractNumber(),
                newReferencedContractNumber
        );

        addChange(
                changes,
                "Дата основного договора",
                document.getReferencedContractDate(),
                request.referencedContractDate()
        );

        document.setDocumentNumber(
                newDocumentNumber
        );

        document.setDocumentDate(
                request.documentDate()
        );

        document.setAmount(
                request.amount()
        );

        document.setCurrency(
                newCurrency
        );

        document.setSubject(
                newSubject
        );

        document.setValidFrom(
                request.validFrom()
        );

        document.setValidTo(
                request.validTo()
        );

        document.setReferencedContractNumber(
                newReferencedContractNumber
        );

        document.setReferencedContractDate(
                request.referencedContractDate()
        );

        Document savedDocument =
                documentRepository.save(
                        document
                );

        if (!changes.isEmpty()) {

            documentAuditService.recordUserEvent(
                    savedDocument,
                    DocumentAuditEventType.METADATA_UPDATED,
                    "Изменены реквизиты документа",
                    String.join(
                            "\n",
                            changes
                    )
            );
        }

        return toResponse(
                savedDocument
        );
    }

    @Transactional(readOnly = true)
    public DocumentMetadataResponse getMetadata(
            Long documentId
    ) {

        Document document =
                getAccessibleDocument(
                        documentId
                );

        return toResponse(
                document
        );
    }

    private Document getAccessibleDocument(
            Long documentId
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                        );

        documentAccessService
                .requireAccess(
                        document
                );

        return document;
    }

    private void validateEditingAllowed(
            Document document
    ) {

        if (
                document.getStatus()
                        == DocumentStatus.APPROVED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Согласованный документ нельзя изменять. Для изменения требуется новый цикл обработки"
            );
        }

        if (
                document.getStatus()
                        == DocumentStatus.REJECTED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Отклонённый документ нельзя изменять"
            );
        }

        if (
                document.getStatus()
                        == DocumentStatus.READY_FOR_SED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже подготовлен к отправке в СЭД. Реквизиты больше нельзя изменять"
            );
        }

        if (
                document.getStatus()
                        == DocumentStatus.SENT_TO_SED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже отправлен в СЭД. Реквизиты больше нельзя изменять"
            );
        }

        if (
                !sedDispatchRepository
                        .findByDocumentIdOrderByCreatedAtDesc(
                                document.getId()
                        )
                        .isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ имеет историю отправки в СЭД. Реквизиты больше нельзя изменять"
            );
        }
    }

    private void validate(
            DocumentMetadataRequest request
    ) {

        BigDecimal amount =
                request.amount();

        if (
                amount != null
                        && amount.compareTo(
                                BigDecimal.ZERO
                        ) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Сумма документа не может быть отрицательной"
            );
        }

        if (
                request.validFrom() != null
                        && request.validTo() != null
                        && request.validFrom()
                                .isAfter(
                                        request.validTo()
                                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Дата начала действия не может быть позже даты окончания действия"
            );
        }

        String currency =
                normalizeCurrency(
                        request.currency()
                );

        if (
                currency != null
                        && currency.length() != 3
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Код валюты должен состоять из 3 символов"
            );
        }
    }

    private void addChange(
            List<String> changes,
            String fieldName,
            Object oldValue,
            Object newValue
    ) {

        if (
                Objects.equals(
                        oldValue,
                        newValue
                )
        ) {
            return;
        }

        changes.add(
                fieldName
                        + ": "
                        + formatValue(oldValue)
                        + " → "
                        + formatValue(newValue)
        );
    }

    private void addAmountChange(
            List<String> changes,
            BigDecimal oldValue,
            BigDecimal newValue
    ) {

        boolean equal;

        if (
                oldValue == null
                        && newValue == null
        ) {
            equal = true;

        } else if (
                oldValue == null
                        || newValue == null
        ) {
            equal = false;

        } else {
            equal =
                    oldValue.compareTo(
                            newValue
                    ) == 0;
        }

        if (equal) {
            return;
        }

        changes.add(
                "Сумма: "
                        + formatValue(oldValue)
                        + " → "
                        + formatValue(newValue)
        );
    }

    private String formatValue(
            Object value
    ) {

        if (value == null) {
            return "не указано";
        }

        if (value instanceof BigDecimal number) {

            return number
                    .stripTrailingZeros()
                    .toPlainString();
        }

        String text =
                value.toString()
                        .trim();

        return text.isEmpty()
                ? "не указано"
                : text;
    }

    private String normalizeText(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private String normalizeCurrency(
            String value
    ) {

        String normalized =
                normalizeText(
                        value
                );

        if (normalized == null) {
            return null;
        }

        return normalized.toUpperCase(
                Locale.ROOT
        );
    }

    private DocumentMetadataResponse toResponse(
            Document document
    ) {

        return new DocumentMetadataResponse(
                document.getId(),
                document.getDocumentType() == null
                        ? null
                        : document.getDocumentType().name(),
                document.getStatus() == null
                        ? null
                        : document.getStatus().name(),
                document.getDocumentNumber(),
                document.getDocumentDate(),
                document.getAmount(),
                document.getCurrency(),
                document.getSubject(),
                document.getValidFrom(),
                document.getValidTo(),
                document.getReferencedContractNumber(),
                document.getReferencedContractDate()
        );
    }
}