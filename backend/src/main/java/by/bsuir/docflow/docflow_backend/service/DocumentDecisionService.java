package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentDecisionRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentDecisionResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentDecisionRecord;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentDecision;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentDecisionRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocumentDecisionService {

    private final DocumentRepository documentRepository;
    private final DocumentDecisionRepository documentDecisionRepository;
    private final AppUserRepository appUserRepository;
    private final DocumentAccessService documentAccessService;

    public DocumentDecisionService(
            DocumentRepository documentRepository,
            DocumentDecisionRepository documentDecisionRepository,
            AppUserRepository appUserRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRepository =
                documentRepository;

        this.documentDecisionRepository =
                documentDecisionRepository;

        this.appUserRepository =
                appUserRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional
    public DocumentDecisionResponse decide(
            Long documentId,
            DocumentDecisionRequest request
    ) {

        if (
                request == null
                        || request.decision() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать решение по документу"
            );
        }

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

        documentAccessService.requireAccess(
                document
        );

        requireDecisionPermission();

        validateCurrentStatus(
                document
        );

        String comment =
                normalizeComment(
                        request.comment()
                );

        validateComment(
                request.decision(),
                comment
        );

        AppUser currentUser =
                resolveCurrentUser();

        DocumentStatus targetStatus =
                resolveTargetStatus(
                        request.decision()
                );

        document.setStatus(
                targetStatus
        );

        documentRepository.save(
                document
        );

        DocumentDecisionRecord record =
                new DocumentDecisionRecord();

        record.setDocument(
                document
        );

        record.setUser(
                currentUser
        );

        record.setDecision(
                request.decision()
        );

        record.setComment(
                comment
        );

        DocumentDecisionRecord savedRecord =
                documentDecisionRepository.save(
                        record
                );

        return new DocumentDecisionResponse(
                savedRecord.getId(),
                document.getId(),
                savedRecord.getDecision().name(),
                document.getStatus().name(),
                savedRecord.getComment(),
                currentUser.getId(),
                currentUser.getFullName(),
                savedRecord.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentDecisionResponse> getHistory(
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

        documentAccessService.requireAccess(
                document
        );

        return documentDecisionRepository
                .findByDocument_IdOrderByCreatedAtAsc(
                        documentId
                )
                .stream()
                .map(
                        record ->
                                new DocumentDecisionResponse(
                                        record.getId(),
                                        documentId,
                                        record.getDecision().name(),
                                        record.getDocument().getStatus().name(),
                                        record.getComment(),
                                        record.getUser().getId(),
                                        record.getUser().getFullName(),
                                        record.getCreatedAt()
                                )
                )
                .toList();
    }
    private void validateCurrentStatus(
            Document document
    ) {

        DocumentStatus status =
                document.getStatus();

        if (
                status != DocumentStatus.VERIFIED
                        && status != DocumentStatus.LINKED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Решение специалиста доступно только для документа, находящегося на этапе проверки"
            );
        }
    }

    private void validateComment(
            DocumentDecision decision,
            String comment
    ) {

        boolean commentRequired =
                decision
                        == DocumentDecision.RETURN_FOR_CORRECTION
                        || decision
                        == DocumentDecision.REJECT;

        if (
                commentRequired
                        && (
                                comment == null
                                        || comment.isBlank()
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "При возврате на доработку или отклонении необходимо указать комментарий"
            );
        }

        if (
                comment != null
                        && comment.length() > 2000
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Комментарий не должен превышать 2000 символов"
            );
        }
    }

    private DocumentStatus resolveTargetStatus(
            DocumentDecision decision
    ) {

        return switch (decision) {

            case APPROVE ->
                    DocumentStatus.APPROVED;

            case RETURN_FOR_CORRECTION ->
                    DocumentStatus.RETURNED_FOR_CORRECTION;

            case REJECT ->
                    DocumentStatus.REJECTED;
        };
    }

    private AppUser resolveCurrentUser() {

        Authentication authentication =
                getAuthentication();

        String login =
                authentication.getName();

        return appUserRepository
                .findByLogin(login)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Текущий пользователь не найден"
                        )
                );
    }

    private void requireDecisionPermission() {

        if (
                hasAuthority("ROLE_ADMIN")
                        || hasAuthority("ROLE_LAWYER")
                        || hasAuthority("ROLE_CONTRACT_MANAGER")
        ) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Текущая роль не может принимать решение по документу"
        );
    }

    private boolean hasAuthority(
            String authorityName
    ) {

        return getAuthentication()
                .getAuthorities()
                .stream()
                .map(
                        GrantedAuthority::getAuthority
                )
                .anyMatch(
                        authorityName::equals
                );
    }

    private Authentication getAuthentication() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Пользователь не авторизован"
            );
        }

        return authentication;
    }

    private String normalizeComment(
            String comment
    ) {

        if (comment == null) {
            return null;
        }

        String normalized =
                comment.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}