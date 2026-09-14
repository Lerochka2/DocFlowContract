package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingTarget;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocumentAccessService {

    private final DocumentRepository documentRepository;
    private final DocumentRouteRepository documentRouteRepository;

    public DocumentAccessService(
            DocumentRepository documentRepository,
            DocumentRouteRepository documentRouteRepository
    ) {
        this.documentRepository =
                documentRepository;

        this.documentRouteRepository =
                documentRouteRepository;
    }

    public List<Document> filterAccessibleDocuments(
            List<Document> documents
    ) {

        if (canSeeAllDocuments()) {
            return documents;
        }

        return documents
                .stream()
                .filter(this::canAccess)
                .toList();
    }

    public boolean canAccess(
            Document document
    ) {

        if (document == null) {
            return false;
        }

        if (canSeeAllDocuments()) {
            return true;
        }

        RoutingTarget requiredTarget =
                resolveCurrentRoutingTarget();

        return hasRoutingTarget(
                document,
                requiredTarget
        );
    }

    public void requireAccess(
            Long documentId
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () ->
                                        new DocumentNotFoundException(
                                                "Документ с ID "
                                                        + documentId
                                                        + " не найден"
                                        )
                        );

        requireAccess(
                document
        );
    }

    public void requireAccess(
            Document document
    ) {

        if (canAccess(document)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Нет доступа к данному документу"
        );
    }

    private boolean hasRoutingTarget(
            Document document,
            RoutingTarget requiredTarget
    ) {

        if (document.getId() == null) {
            return false;
        }

        return documentRouteRepository
                .findFirstByDocumentIdOrderByCreatedAtDesc(
                        document.getId()
                )
                .map(
                        route ->
                                route.getRoutingTarget()
                                        == requiredTarget
                )
                .orElse(false);
    }

    private boolean canSeeAllDocuments() {

        return hasAuthority(
                "ROLE_ADMIN"
        )
                || hasAuthority(
                        "ROLE_DOCUMENT_OPERATOR"
                );
    }

    private RoutingTarget resolveCurrentRoutingTarget() {

        if (
                hasAuthority(
                        "ROLE_LAWYER"
                )
        ) {
            return RoutingTarget.LAWYER;
        }

        if (
                hasAuthority(
                        "ROLE_CONTRACT_MANAGER"
                )
        ) {
            return RoutingTarget.CONTRACT_MANAGER;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Для текущей роли доступ к документам не настроен"
        );
    }

    private boolean hasAuthority(
            String authorityName
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .map(
                        GrantedAuthority::getAuthority
                )
                .anyMatch(
                        authorityName::equals
                );
    }
}