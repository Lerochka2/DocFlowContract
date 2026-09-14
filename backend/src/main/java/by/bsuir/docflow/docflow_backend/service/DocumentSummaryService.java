package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentSummaryResponse;
import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.SedDispatch;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AiProcessingResultRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import by.bsuir.docflow.docflow_backend.repository.SedDispatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class DocumentSummaryService {

    private final DocumentRepository documentRepository;
    private final AiProcessingResultRepository aiProcessingResultRepository;
    private final DocumentRouteRepository documentRouteRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final SedDispatchRepository sedDispatchRepository;
    private final DocumentAccessService documentAccessService;

    public DocumentSummaryService(
            DocumentRepository documentRepository,
            AiProcessingResultRepository aiProcessingResultRepository,
            DocumentRouteRepository documentRouteRepository,
            DocumentLinkRepository documentLinkRepository,
            SedDispatchRepository sedDispatchRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRepository =
                documentRepository;

        this.aiProcessingResultRepository =
                aiProcessingResultRepository;

        this.documentRouteRepository =
                documentRouteRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.sedDispatchRepository =
                sedDispatchRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional(readOnly = true)
    public DocumentSummaryResponse getSummary(
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

        AiProcessingResult latestAi =
                aiProcessingResultRepository
                        .findAll()
                        .stream()
                        .filter(
                                result ->
                                        result.getDocument() != null
                                                && documentId.equals(
                                                        result
                                                                .getDocument()
                                                                .getId()
                                                )
                        )
                        .max(
                                Comparator.comparing(
                                        AiProcessingResult::getId
                                )
                        )
                        .orElse(null);

        DocumentRoute latestRoute =
                documentRouteRepository
                        .findFirstByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .orElse(null);

        DocumentLink documentLink =
                documentLinkRepository
                        .findByChildDocumentId(
                                documentId
                        )
                        .orElse(null);

        SedDispatch latestDispatch =
                sedDispatchRepository
                        .findFirstByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .orElse(null);

        int linkedDocumentsCount =
                (int) documentLinkRepository
                        .findByContractDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .stream()
                        .filter(
                                link ->
                                        documentAccessService
                                                .canAccess(
                                                        link.getChildDocument()
                                                )
                        )
                        .count();

        return new DocumentSummaryResponse(
                document.getId(),

                document.getFile() == null
                        ? null
                        : document.getFile().getId(),

                document.getFile() == null
                        ? null
                        : document.getFile().getOriginalName(),

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

                toAiSummary(
                        latestAi
                ),

                toRoutingSummary(
                        latestRoute
                ),

                toLinkSummary(
                        documentLink
                ),

                toSedSummary(
                        latestDispatch
                ),

                linkedDocumentsCount
        );
    }

    private DocumentSummaryResponse.AiSummary toAiSummary(
            AiProcessingResult result
    ) {

        if (result == null) {
            return null;
        }

        return new DocumentSummaryResponse.AiSummary(
                result.getId(),

                result.getPredictedType() == null
                        ? null
                        : result.getPredictedType().name(),

                result.getConfidence(),

                result.getProcessingStatus() == null
                        ? null
                        : result.getProcessingStatus().name()
        );
    }

    private DocumentSummaryResponse.RoutingSummary toRoutingSummary(
            DocumentRoute route
    ) {

        if (route == null) {
            return null;
        }

        return new DocumentSummaryResponse.RoutingSummary(
                route.getId(),

                route.getRoutingTarget() == null
                        ? null
                        : route.getRoutingTarget().name(),

                route.getRoutingStatus() == null
                        ? null
                        : route.getRoutingStatus().name(),

                route.getRoutingReason(),
                route.getRoutedAt()
        );
    }

    private DocumentSummaryResponse.LinkSummary toLinkSummary(
            DocumentLink link
    ) {

        if (link == null) {
            return null;
        }

        Document contract =
                link.getContractDocument();

        return new DocumentSummaryResponse.LinkSummary(
                link.getId(),
                contract.getId(),
                contract.getDocumentNumber(),
                contract.getDocumentDate(),
                link.getLinkedAt()
        );
    }

    private DocumentSummaryResponse.SedSummary toSedSummary(
            SedDispatch dispatch
    ) {

        if (dispatch == null) {
            return null;
        }

        return new DocumentSummaryResponse.SedSummary(
                dispatch.getId(),

                dispatch.getDispatchStatus() == null
                        ? null
                        : dispatch.getDispatchStatus().name(),

                dispatch.getExternalDocumentId(),
                dispatch.getResponseMessage(),
                dispatch.getSentAt()
        );
    }
}