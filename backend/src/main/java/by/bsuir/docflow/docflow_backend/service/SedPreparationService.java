package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.SedPreparationResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SedPreparationService {

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentRouteRepository documentRouteRepository;
    private final DocumentAccessService documentAccessService;

    public SedPreparationService(
            DocumentRepository documentRepository,
            DocumentLinkRepository documentLinkRepository,
            DocumentRouteRepository documentRouteRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.documentRouteRepository =
                documentRouteRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional
    public SedPreparationResponse prepare(
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

        if (document.getDocumentType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно подготовить документ к отправке: тип документа не определен"
            );
        }

        validateStatus(
                document
        );

        validateMetadata(
                document
        );

        validateLink(
                document
        );

        DocumentRoute activeRoute =
                documentRouteRepository
                        .findFirstByDocumentIdAndRoutingStatusOrderByCreatedAtDesc(
                                documentId,
                                RoutingStatus.ROUTED
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Невозможно подготовить документ к отправке: активный маршрут отсутствует"
                                )
                        );

        document.setStatus(
                DocumentStatus.READY_FOR_SED
        );

        Document savedDocument =
                documentRepository.save(
                        document
                );

        return new SedPreparationResponse(
                savedDocument.getId(),
                savedDocument.getDocumentType().name(),
                savedDocument.getStatus().name(),
                savedDocument.getDocumentNumber(),
                savedDocument.getDocumentDate(),
                savedDocument.getSubject(),
                activeRoute.getRoutingTarget().name()
        );
    }

    private void validateStatus(
            Document document
    ) {

        if (
                document.getStatus()
                        != DocumentStatus.APPROVED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ должен быть согласован специалистом перед подготовкой к отправке в СЭД"
            );
        }
    }
    private void validateMetadata(
            Document document
    ) {

        if (
                isBlank(
                        document.getDocumentNumber()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Не заполнен номер документа"
            );
        }

        if (document.getDocumentDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Не заполнена дата документа"
            );
        }

        if (
                isBlank(
                        document.getSubject()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Не заполнен предмет документа"
            );
        }

        boolean amountSpecified =
                document.getAmount() != null;

        boolean currencySpecified =
                !isBlank(
                        document.getCurrency()
                );

        if (
                amountSpecified
                        && !currencySpecified
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для указанной суммы необходимо указать валюту"
            );
        }

        if (
                !amountSpecified
                        && currencySpecified
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для указанной валюты необходимо указать сумму"
            );
        }
    }

    private void validateLink(
            Document document
    ) {

        if (
                document.getDocumentType()
                        == DocumentType.CONTRACT
        ) {
            return;
        }

        boolean linkExists =
                documentLinkRepository
                        .existsByChildDocumentId(
                                document.getId()
                        );

        if (!linkExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ не привязан к основному договору"
            );
        }
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }
}