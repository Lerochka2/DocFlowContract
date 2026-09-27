package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentAuditEventResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentAuditEvent;
import by.bsuir.docflow.docflow_backend.entity.enums.AuditActorType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentAuditEventRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DocumentAuditService {

    private static final String SYSTEM_ACTOR_NAME =
            "Система DocFlow";

    private final DocumentRepository documentRepository;
    private final DocumentAuditEventRepository auditRepository;
    private final AppUserRepository appUserRepository;
    private final DocumentAccessService documentAccessService;

    public DocumentAuditService(
            DocumentRepository documentRepository,
            DocumentAuditEventRepository auditRepository,
            AppUserRepository appUserRepository,
            DocumentAccessService documentAccessService
    ) {
        this.documentRepository =
                documentRepository;

        this.auditRepository =
                auditRepository;

        this.appUserRepository =
                appUserRepository;

        this.documentAccessService =
                documentAccessService;
    }

    @Transactional(readOnly = true)
    public List<DocumentAuditEventResponse> getHistory(
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

        documentAccessService.requireAccess(
                document
        );

        List<DocumentAuditEvent> history =
                auditRepository
                        .findByDocument_IdOrderByCreatedAtAsc(
                                documentId
                        );


        List<DocumentAuditEvent> cleanedHistory =
                new ArrayList<>();


        for (
                DocumentAuditEvent event
                : history
        ) {

            if (!cleanedHistory.isEmpty()) {

                DocumentAuditEvent previous =
                        cleanedHistory.get(
                                cleanedHistory.size() - 1
                        );


                if (
                        isDuplicateRiskEvent(
                                previous,
                                event
                        )
                ) {

                    continue;
                }
            }


            cleanedHistory.add(
                    event
            );
        }


        return cleanedHistory
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    @Transactional
    public void recordUserEvent(
            Document document,
            DocumentAuditEventType eventType,
            String description
    ) {

        recordUserEvent(
                document,
                eventType,
                description,
                null
        );
    }

    @Transactional
    public void recordUserEvent(
            Document document,
            DocumentAuditEventType eventType,
            String description,
            String details
    ) {

        AppUser currentUser =
                resolveCurrentUser();

        saveEvent(
                document,
                currentUser,
                AuditActorType.USER,
                currentUser.getFullName(),
                eventType,
                description,
                details
        );
    }

    @Transactional
    public void recordSystemEvent(
            Document document,
            DocumentAuditEventType eventType,
            String description
    ) {

        recordSystemEvent(
                document,
                eventType,
                description,
                null
        );
    }

    @Transactional
    public void recordSystemEvent(
            Document document,
            DocumentAuditEventType eventType,
            String description,
            String details
    ) {

        saveEvent(
                document,
                null,
                AuditActorType.SYSTEM,
                SYSTEM_ACTOR_NAME,
                eventType,
                description,
                details
        );
    }

    private void saveEvent(
            Document document,
            AppUser user,
            AuditActorType actorType,
            String actorName,
            DocumentAuditEventType eventType,
            String description,
            String details
    ) {

        if (document == null) {
            throw new IllegalArgumentException(
                    "Document must not be null"
            );
        }

        if (eventType == null) {
            throw new IllegalArgumentException(
                    "Audit event type must not be null"
            );
        }

        if (
                description == null
                        || description.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Audit description must not be blank"
            );
        }

        String normalizedDescription =
                description.trim();

        String normalizedDetails =
                normalizeDetails(
                        details
                );


        if (
                eventType
                        == DocumentAuditEventType.RISK_ANALYSIS_COMPLETED
                        &&
                isRecentDuplicateRiskEvent(
                        document,
                        actorType,
                        actorName,
                        normalizedDescription,
                        normalizedDetails
                )
        ) {

            return;
        }


        DocumentAuditEvent event =
                new DocumentAuditEvent();

        event.setDocument(
                document
        );

        event.setUser(
                user
        );

        event.setActorType(
                actorType
        );

        event.setActorName(
                actorName
        );

        event.setEventType(
                eventType
        );

        event.setDescription(
                normalizedDescription
        );

        event.setDetails(
                normalizedDetails
        );

        auditRepository.save(
                event
        );
    }

    private boolean isDuplicateRiskEvent(
            DocumentAuditEvent previous,
            DocumentAuditEvent current
    ) {

        if (
                previous == null
                        ||
                current == null
        ) {

            return false;
        }


        if (
                previous.getEventType()
                        != DocumentAuditEventType.RISK_ANALYSIS_COMPLETED
                        ||
                current.getEventType()
                        != DocumentAuditEventType.RISK_ANALYSIS_COMPLETED
        ) {

            return false;
        }


        if (
                previous.getActorType()
                        != current.getActorType()
        ) {

            return false;
        }


        if (
                !Objects.equals(
                        previous.getActorName(),
                        current.getActorName()
                )
                        ||
                !Objects.equals(
                        previous.getDescription(),
                        current.getDescription()
                )
                        ||
                !Objects.equals(
                        previous.getDetails(),
                        current.getDetails()
                )
        ) {

            return false;
        }


        if (
                previous.getCreatedAt() == null
                        ||
                current.getCreatedAt() == null
        ) {

            return false;
        }


        long seconds =
                Duration
                        .between(
                                previous.getCreatedAt(),
                                current.getCreatedAt()
                        )
                        .abs()
                        .toSeconds();


        return seconds <= 60;
    }


    private boolean isRecentDuplicateRiskEvent(
            Document document,
            AuditActorType actorType,
            String actorName,
            String description,
            String details
    ) {

        if (
                document == null
                        ||
                document.getId() == null
        ) {

            return false;
        }


        List<DocumentAuditEvent> history =
                auditRepository
                        .findByDocument_IdOrderByCreatedAtAsc(
                                document.getId()
                        );


        if (history.isEmpty()) {

            return false;
        }


        DocumentAuditEvent previous =
                history.get(
                        history.size() - 1
                );


        if (
                previous.getEventType()
                        != DocumentAuditEventType.RISK_ANALYSIS_COMPLETED
        ) {

            return false;
        }


        if (
                previous.getActorType()
                        != actorType
        ) {

            return false;
        }


        if (
                !Objects.equals(
                        previous.getActorName(),
                        actorName
                )
                        ||
                !Objects.equals(
                        previous.getDescription(),
                        description
                )
                        ||
                !Objects.equals(
                        previous.getDetails(),
                        details
                )
        ) {

            return false;
        }


        if (
                previous.getCreatedAt() == null
        ) {

            return false;
        }


        long seconds =
                Duration
                        .between(
                                previous.getCreatedAt(),
                                java.time.LocalDateTime.now()
                        )
                        .abs()
                        .toSeconds();


        return seconds <= 60;
    }

    private DocumentAuditEventResponse toResponse(
            DocumentAuditEvent event
    ) {

        Long userId =
                event.getUser() == null
                        ? null
                        : event.getUser().getId();

        return new DocumentAuditEventResponse(
                event.getId(),
                event.getDocument().getId(),
                event.getEventType().name(),
                event.getActorType().name(),
                userId,
                event.getActorName(),
                event.getDescription(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }

    private AppUser resolveCurrentUser() {

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

        return appUserRepository
                .findByLogin(
                        authentication.getName()
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Текущий пользователь не найден"
                                )
                );
    }

    private String normalizeDetails(
            String details
    ) {

        if (details == null) {
            return null;
        }

        String normalized =
                details.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}