package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentLinkDetailsResponse;
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

import java.util.List;

@Service
public class DocumentLinkQueryService {

    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentRepository documentRepository;

    public DocumentLinkQueryService(
            DocumentLinkRepository documentLinkRepository,
            DocumentRepository documentRepository
    ) {
        this.documentLinkRepository =
                documentLinkRepository;
        this.documentRepository =
                documentRepository;
    }

    @Transactional(readOnly = true)
    public DocumentLinkDetailsResponse getContractLink(
            Long childDocumentId
    ) {

        ensureDocumentExists(
                childDocumentId
        );

        DocumentLink link =
                documentLinkRepository
                        .findByChildDocumentId(
                                childDocumentId
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Для документа с ID "
                                                + childDocumentId
                                                + " связь с основным договором отсутствует"
                                )
                        );

        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<DocumentLinkDetailsResponse> getLinkedDocuments(
            Long contractDocumentId
    ) {

        Document contractDocument =
                documentRepository
                        .findById(
                                contractDocumentId
                        )
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + contractDocumentId
                                                + " не найден"
                                )
                        );

        if (
                contractDocument.getDocumentType()
                        != DocumentType.CONTRACT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Список связанных документов доступен только для документа типа CONTRACT"
            );
        }

        return documentLinkRepository
                .findByContractDocumentIdOrderByCreatedAtDesc(
                        contractDocumentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureDocumentExists(
            Long documentId
    ) {

        if (!documentRepository.existsById(
                documentId
        )) {
            throw new DocumentNotFoundException(
                    "Документ с ID "
                            + documentId
                            + " не найден"
            );
        }
    }

    private DocumentLinkDetailsResponse toResponse(
            DocumentLink link
    ) {

        Document child =
                link.getChildDocument();

        Document contract =
                link.getContractDocument();

        return new DocumentLinkDetailsResponse(
                link.getId(),
                child.getId(),
                child.getDocumentType() == null
                        ? null
                        : child.getDocumentType().name(),
                child.getStatus() == null
                        ? null
                        : child.getStatus().name(),
                contract.getId(),
                contract.getDocumentType() == null
                        ? null
                        : contract.getDocumentType().name(),
                contract.getStatus() == null
                        ? null
                        : contract.getStatus().name(),
                link.getLinkedAt()
        );
    }
}