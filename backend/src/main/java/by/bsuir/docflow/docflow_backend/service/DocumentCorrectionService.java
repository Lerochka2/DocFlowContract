package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentApprovalStep;
import by.bsuir.docflow.docflow_backend.entity.DocumentVersion;
import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentApprovalStepRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentVersionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentCorrectionService {

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentApprovalStepRepository approvalStepRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentAuditService documentAuditService;
    private final DocumentRoutingService documentRoutingService;


    public DocumentCorrectionService(
            DocumentRepository documentRepository,
            DocumentLinkRepository documentLinkRepository,
            DocumentApprovalStepRepository approvalStepRepository,
            DocumentVersionRepository documentVersionRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService,
            DocumentRoutingService documentRoutingService
    ) {

        this.documentRepository =
                documentRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.approvalStepRepository =
                approvalStepRepository;

        this.documentVersionRepository =
                documentVersionRepository;

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


        requireNewVersionAfterReturn(
                document
        );


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

    private void requireNewVersionAfterReturn(
            Document document
    ) {

        if (
                document.getFile() == null
                        ||
                document.getFile()
                        .getId() == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "У документа отсутствует текущий файл"
            );
        }

        DocumentVersion currentVersion =
                documentVersionRepository
                        .findByDocumentIdAndFileId(
                                document.getId(),
                                document.getFile()
                                        .getId()
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Текущая версия документа не зарегистрирована"
                                        )
                        );

        DocumentApprovalStep latestReturnedStep =
                null;

        for (
                DocumentApprovalStep step
                : approvalStepRepository
                        .findByDocumentIdOrderByStepOrderAsc(
                                document.getId()
                        )
        ) {

            if (
                    step.getStatus()
                            == ApprovalStepStatus.RETURNED
            ) {

                latestReturnedStep =
                        step;
            }
        }

        if (latestReturnedStep == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Не найден этап, на котором документ был возвращён на доработку"
            );
        }

        DocumentVersion returnedVersion =
                latestReturnedStep
                        .getDocumentVersion();

        if (returnedVersion == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Для возвращённого этапа не определена версия документа"
            );
        }

        if (
                returnedVersion.getId()
                        .equals(
                                currentVersion.getId()
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Перед повторной отправкой необходимо загрузить новую версию документа"
            );
        }
    }
}