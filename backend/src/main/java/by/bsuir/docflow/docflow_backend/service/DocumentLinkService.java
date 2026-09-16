package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentLinkRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentLinkResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
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
public class DocumentLinkService {

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentRoutingService documentRoutingService;
    private final DocumentAuditService documentAuditService;

    public DocumentLinkService(
            DocumentRepository documentRepository,
            DocumentLinkRepository documentLinkRepository,
            DocumentRoutingService documentRoutingService,
            DocumentAuditService documentAuditService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.documentRoutingService =
                documentRoutingService;

        this.documentAuditService =
                documentAuditService;
    }

    @Transactional
    public DocumentLinkResponse link(
            Long childDocumentId,
            DocumentLinkRequest request
    ) {

        if (
                request == null
                        || request.contractDocumentId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать ID основного договора"
            );
        }

        Document childDocument =
                documentRepository
                        .findById(
                                childDocumentId
                        )
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Документ с ID "
                                                + childDocumentId
                                                + " не найден"
                                )
                        );

        Document contractDocument =
                documentRepository
                        .findById(
                                request.contractDocumentId()
                        )
                        .orElseThrow(
                                () -> new DocumentNotFoundException(
                                        "Основной договор с ID "
                                                + request.contractDocumentId()
                                                + " не найден"
                                )
                        );

        if (
                childDocument.getId()
                        .equals(
                                contractDocument.getId()
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Документ нельзя привязать к самому себе"
            );
        }

        if (
                childDocument.getStatus()
                        != DocumentStatus.LINK_REQUIRED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Привязка доступна только для документов со статусом LINK_REQUIRED"
            );
        }

        if (!requiresContractLink(
                childDocument.getDocumentType()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Для данного типа документа привязка к договору не требуется"
            );
        }

        if (
                contractDocument.getDocumentType()
                        != DocumentType.CONTRACT
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Выбранный основной документ должен иметь тип CONTRACT"
            );
        }

        if (!isAvailableContractStatus(
                contractDocument.getStatus()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Выбранный договор еще не готов для привязки документов"
            );
        }

        if (
                documentLinkRepository
                        .existsByChildDocumentId(
                                childDocumentId
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже привязан к основному договору"
            );
        }

        DocumentLink link =
                new DocumentLink();

        link.setChildDocument(
                childDocument
        );

        link.setContractDocument(
                contractDocument
        );

        DocumentLink savedLink =
                documentLinkRepository.save(
                        link
                );

        childDocument.setStatus(
                DocumentStatus.LINKED
        );

        documentRepository.save(
                childDocument
        );

        DocumentRoute route =
                documentRoutingService.route(
                        childDocument
                );

        documentAuditService.recordUserEvent(
                childDocument,
                DocumentAuditEventType.DOCUMENT_LINKED,
                "Документ привязан к основному договору",
                buildContractDetails(
                        contractDocument
                )
        );

        return new DocumentLinkResponse(
                savedLink.getId(),
                childDocument.getId(),
                childDocument.getDocumentType().name(),
                contractDocument.getId(),
                contractDocument.getDocumentType().name(),
                childDocument.getStatus().name(),
                route.getRoutingTarget().name(),
                route.getRoutingStatus().name(),
                savedLink.getLinkedAt()
        );
    }

    private String buildContractDetails(
            Document contractDocument
    ) {

        StringBuilder details =
                new StringBuilder();

        details.append(
                "Основной договор: "
        );

        if (
                contractDocument.getDocumentNumber() != null
                        && !contractDocument.getDocumentNumber().isBlank()
        ) {
            details.append("№ ")
                    .append(
                            contractDocument.getDocumentNumber()
                    );
        } else {
            details.append(
                    "номер не указан"
            );
        }

        if (
                contractDocument.getDocumentDate() != null
        ) {
            details.append(" от ")
                    .append(
                            contractDocument.getDocumentDate()
                    );
        }

        details.append("\nID договора: ")
                .append(
                        contractDocument.getId()
                );

        return details.toString();
    }

    private boolean requiresContractLink(
            DocumentType documentType
    ) {

        if (documentType == null) {
            return false;
        }

        return documentType
                == DocumentType.AMENDMENT_CHANGE
                || documentType
                == DocumentType.AMENDMENT_PROLONGATION
                || documentType
                == DocumentType.APPENDIX;
    }

    private boolean isAvailableContractStatus(
            DocumentStatus status
    ) {

        if (status == null) {
            return false;
        }

        return status == DocumentStatus.VERIFIED
                || status == DocumentStatus.APPROVED
                || status == DocumentStatus.READY_FOR_SED
                || status == DocumentStatus.SENT_TO_SED;
    }
}