package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentMetadataRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentMetadataResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class DocumentMetadataService {

    private final DocumentRepository documentRepository;

    public DocumentMetadataService(
            DocumentRepository documentRepository
    ) {
        this.documentRepository =
                documentRepository;
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
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                        );

        validate(request);

        document.setDocumentNumber(
                normalizeText(
                        request.documentNumber()
                )
        );

        document.setDocumentDate(
                request.documentDate()
        );

        document.setAmount(
                request.amount()
        );

        document.setCurrency(
                normalizeCurrency(
                        request.currency()
                )
        );

        document.setSubject(
                normalizeText(
                        request.subject()
                )
        );

        document.setValidFrom(
                request.validFrom()
        );

        document.setValidTo(
                request.validTo()
        );

        document.setReferencedContractNumber(
                normalizeText(
                        request.referencedContractNumber()
                )
        );

        document.setReferencedContractDate(
                request.referencedContractDate()
        );

        Document savedDocument =
                documentRepository.save(
                        document
                );

        return toResponse(
                savedDocument
        );
    }

    @Transactional(readOnly = true)
    public DocumentMetadataResponse getMetadata(
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

        return toResponse(
                document
        );
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
                normalizeText(value);

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