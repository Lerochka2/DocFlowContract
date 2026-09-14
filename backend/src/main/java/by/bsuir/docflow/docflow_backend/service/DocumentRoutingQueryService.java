package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentRouteResponse;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocumentRoutingQueryService {

    private final DocumentRouteRepository documentRouteRepository;
    private final DocumentAccessService documentAccessService;

    public DocumentRoutingQueryService(
            DocumentRouteRepository documentRouteRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRouteRepository =
                documentRouteRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional(readOnly = true)
    public List<DocumentRouteResponse> getHistory(
            Long documentId
    ) {

        documentAccessService
                .requireAccess(
                        documentId
                );

        return documentRouteRepository
                .findByDocumentIdOrderByCreatedAtDesc(
                        documentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentRouteResponse getLatest(
            Long documentId
    ) {

        documentAccessService
                .requireAccess(
                        documentId
                );

        DocumentRoute route =
                documentRouteRepository
                        .findFirstByDocumentIdOrderByCreatedAtDesc(
                                documentId
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Для документа с ID "
                                                + documentId
                                                + " маршруты отсутствуют"
                                )
                        );

        return toResponse(
                route
        );
    }

    private DocumentRouteResponse toResponse(
            DocumentRoute route
    ) {

        return new DocumentRouteResponse(
                route.getId(),
                route.getDocument().getId(),
                route.getDocumentType() == null
                        ? null
                        : route.getDocumentType().name(),
                route.getRoutingTarget() == null
                        ? null
                        : route.getRoutingTarget().name(),
                route.getRoutingStatus() == null
                        ? null
                        : route.getRoutingStatus().name(),
                route.getRoutingReason(),
                route.getRoutedAt(),
                route.getCreatedAt()
        );
    }
}