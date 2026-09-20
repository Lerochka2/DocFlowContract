package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentArchiveResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentArchiveService {

    private final DocumentRepository documentRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentAuditService documentAuditService;

    public DocumentArchiveService(
            DocumentRepository documentRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentAccessService =
                documentAccessService;

        this.documentAuditService =
                documentAuditService;
    }

    @Transactional
    public DocumentArchiveResponse archive(
            Long documentId
    ) {

        Document document =
                findDocument(
                        documentId
                );

        documentAccessService.requireAccess(
                document
        );

        if (
                document.getStatus()
                        == DocumentStatus.ARCHIVED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже находится в архиве"
            );
        }

        if (
                document.getStatus()
                        != DocumentStatus.SENT_TO_SED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "В архив можно переместить только документ, отправленный в СЭД"
            );
        }

        document.setStatus(
                DocumentStatus.ARCHIVED
        );

        Document savedDocument =
                documentRepository.save(
                        document
                );

        documentAuditService.recordUserEvent(
                savedDocument,
                DocumentAuditEventType.DOCUMENT_ARCHIVED,
                "Документ перемещён в архив",
                "Предыдущий статус: SENT_TO_SED"
        );

        return toResponse(
                savedDocument
        );
    }

    @Transactional
    public DocumentArchiveResponse restore(
            Long documentId
    ) {

        Document document =
                findDocument(
                        documentId
                );

        documentAccessService.requireAccess(
                document
        );

        if (
                document.getStatus()
                        != DocumentStatus.ARCHIVED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ не находится в архиве"
            );
        }

        document.setStatus(
                DocumentStatus.SENT_TO_SED
        );

        Document savedDocument =
                documentRepository.save(
                        document
                );

        documentAuditService.recordUserEvent(
                savedDocument,
                DocumentAuditEventType.DOCUMENT_RESTORED,
                "Документ восстановлен из архива",
                "Восстановленный статус: SENT_TO_SED"
        );

        return toResponse(
                savedDocument
        );
    }

    private Document findDocument(
            Long documentId
    ) {

        return documentRepository
                .findById(
                        documentId
                )
                .orElseThrow(
                        () ->
                                new DocumentNotFoundException(
                                        "Документ с ID "
                                                + documentId
                                                + " не найден"
                                )
                );
    }

    private DocumentArchiveResponse toResponse(
            Document document
    ) {

        return new DocumentArchiveResponse(
                document.getId(),
                document.getStatus().name(),
                document.getUpdatedAt()
        );
    }
}