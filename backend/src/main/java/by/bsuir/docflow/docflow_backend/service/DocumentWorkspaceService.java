package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentWorkspaceItemResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.ContractExpirationStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class DocumentWorkspaceService {

    private final DocumentRepository documentRepository;
    private final DocumentAccessService documentAccessService;
    private final ContractExpirationService contractExpirationService;

    public DocumentWorkspaceService(
            DocumentRepository documentRepository,
            DocumentAccessService documentAccessService,
            ContractExpirationService contractExpirationService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentAccessService =
                documentAccessService;

        this.contractExpirationService =
                contractExpirationService;
    }

    @Transactional(readOnly = true)
    public List<DocumentWorkspaceItemResponse> search(
            String query,
            String type,
            String status,
            String expirationStatus
    ) {

        DocumentType documentType =
                parseDocumentType(
                        type
                );

        DocumentStatus documentStatus =
                parseDocumentStatus(
                        status
                );

        ContractExpirationStatus parsedExpirationStatus =
                parseExpirationStatus(
                        expirationStatus
                );

        String normalizedQuery =
                normalizeSearchQuery(
                        query
                );

        List<Document> accessibleDocuments =
                documentAccessService
                        .filterAccessibleDocuments(
                                documentRepository.findAll()
                        );

        return accessibleDocuments
                .stream()
                .filter(
                        document ->
                                documentType == null
                                        || document.getDocumentType()
                                        == documentType
                )
                .filter(
                        document ->
                                documentStatus == null
                                        || document.getStatus()
                                        == documentStatus
                )
                .filter(
                        document ->
                                parsedExpirationStatus == null
                                        || contractExpirationService
                                                .calculate(document)
                                                .status()
                                        == parsedExpirationStatus
                )
                .filter(
                        document ->
                                matchesQuery(
                                        document,
                                        normalizedQuery
                                )
                )
                .sorted(
                        Comparator
                                .comparing(
                                        Document::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                                .reversed()
                )
                .map(
                        this::toResponse
                )
                .toList();
    }

    private boolean matchesQuery(
            Document document,
            String query
    ) {

        if (query == null) {
            return true;
        }

        String originalName =
                document.getFile() == null
                        ? null
                        : document.getFile()
                                .getOriginalName();

        return containsIgnoreCase(
                originalName,
                query
        )
                || containsIgnoreCase(
                        document.getDocumentNumber(),
                        query
                )
                || containsIgnoreCase(
                        document.getSubject(),
                        query
                );
    }

    private boolean containsIgnoreCase(
            String value,
            String query
    ) {

        if (value == null) {
            return false;
        }

        return value
                .toLowerCase(
                        Locale.ROOT
                )
                .contains(
                        query
                );
    }

    private String normalizeSearchQuery(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private DocumentType parseDocumentType(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        try {

            return DocumentType.valueOf(
                    value
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Неизвестный тип документа: "
                            + value
            );
        }
    }

    private DocumentStatus parseDocumentStatus(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        try {

            return DocumentStatus.valueOf(
                    value
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Неизвестный статус документа: "
                            + value
            );
        }
    }

    private ContractExpirationStatus parseExpirationStatus(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        try {

            return ContractExpirationStatus.valueOf(
                    value
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Неизвестный статус срока действия: "
                            + value
            );
        }
    }

    private DocumentWorkspaceItemResponse toResponse(
            Document document
    ) {

        ContractExpirationService.ExpirationInfo expirationInfo =
                contractExpirationService
                        .calculate(
                                document
                        );

        return new DocumentWorkspaceItemResponse(
                document.getId(),

                document.getFile() == null
                        ? null
                        : document.getFile().getId(),

                document.getFile() == null
                        ? null
                        : document.getFile()
                                .getOriginalName(),

                document.getDocumentType() == null
                        ? null
                        : document.getDocumentType()
                                .name(),

                document.getStatus() == null
                        ? null
                        : document.getStatus()
                                .name(),

                document.getDocumentNumber(),
                document.getDocumentDate(),
                document.getSubject(),
                document.getAmount(),
                document.getCurrency(),

                expirationInfo.status() == null
                        ? null
                        : expirationInfo.status().name(),

                expirationInfo.daysUntilExpiration(),

                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}