package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentCorrectionService {

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentAuditService documentAuditService;
    private final DocumentRoutingService documentRoutingService;


    public DocumentCorrectionService(
            DocumentRepository documentRepository,
            DocumentLinkRepository documentLinkRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService,
            DocumentRoutingService documentRoutingService
    ) {

        this.documentRepository =
                documentRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.documentAccessService =
                documentAccessService;

        this.documentAuditService =
                documentAuditService;

        this.documentRoutingService =
                documentRoutingService;
    }


    @Transactional
    public DocumentStatus resubmitForReview(
            Long documentId
    ) {

        Document document =
                documentRepository
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


        documentAccessService.requireAccess(
                document
        );


        if (
                document.getStatus()
                        != DocumentStatus.RETURNED_FOR_CORRECTION
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Повторно направить на проверку можно только документ, возвращённый на доработку"
            );
        }


        if (
                document.getDocumentType()
                        == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Тип документа не определён"
            );
        }


        DocumentStatus targetStatus;


        if (
                document.getDocumentType()
                        == DocumentType.CONTRACT
        ) {

            targetStatus =
                    DocumentStatus.VERIFIED;

        } else {

            boolean linkExists =
                    documentLinkRepository
                            .existsByChildDocumentId(
                                    document.getId()
                            );


            if (!linkExists) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Перед повторной отправкой на проверку документ должен быть привязан к основному договору"
                );
            }


            targetStatus =
                    DocumentStatus.LINKED;
        }


        document.setStatus(
                targetStatus
        );


        documentRepository.save(
                document
        );


        /*
         * Повторно запускаем маршрутизацию.
         *
         * Старый этап согласования остаётся
         * RETURNED в истории.
         *
         * ApprovalWorkflowService создаст
         * новый этап PENDING.
         */
        documentRoutingService.route(
                document
        );


        documentAuditService.recordUserEvent(
                document,
                DocumentAuditEventType.RESUBMITTED_FOR_REVIEW,
                "Документ повторно направлен специалисту на проверку",
                "Новый статус: "
                        + targetStatus.name()
        );


        return targetStatus;
    }
}