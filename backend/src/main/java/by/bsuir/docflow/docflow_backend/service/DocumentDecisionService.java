package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.DocumentDecisionRequest;
import by.bsuir.docflow.docflow_backend.dto.DocumentDecisionResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentApprovalStep;
import by.bsuir.docflow.docflow_backend.entity.DocumentDecisionRecord;
import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentDecision;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.exception.DocumentNotFoundException;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentApprovalStepRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentDecisionRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentDecisionService {

    private final DocumentRepository documentRepository;

    private final DocumentDecisionRepository documentDecisionRepository;

    private final AppUserRepository appUserRepository;

    private final DocumentAccessService documentAccessService;

    private final DocumentAuditService documentAuditService;

    private final DocumentApprovalStepRepository approvalStepRepository;


    public DocumentDecisionService(
            DocumentRepository documentRepository,
            DocumentDecisionRepository documentDecisionRepository,
            AppUserRepository appUserRepository,
            DocumentAccessService documentAccessService,
            DocumentAuditService documentAuditService,
            DocumentApprovalStepRepository approvalStepRepository
    ) {

        this.documentRepository =
                documentRepository;

        this.documentDecisionRepository =
                documentDecisionRepository;

        this.appUserRepository =
                appUserRepository;

        this.documentAccessService =
                documentAccessService;

        this.documentAuditService =
                documentAuditService;

        this.approvalStepRepository =
                approvalStepRepository;


    }


    @Transactional
    public DocumentDecisionResponse decide(
            Long documentId,
            DocumentDecisionRequest request
    ) {

        if (
                request == null
                        ||
                request.decision() == null
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
                                () ->
                                        new DocumentNotFoundException(
                                                "Документ с ID "
                                                        + documentId
                                                        + " не найден"
                                        )
                        );

        documentAccessService
                .requireAccess(
                        document
                );

        requireDecisionPermission();

        validateCurrentStatus(
                document
        );

        AppUser currentUser =
                resolveCurrentUser();

        DocumentApprovalStep approvalStep =
                resolveCurrentApprovalStep(
                        documentId,
                        currentUser
                );

        String comment =
                normalizeComment(
                        request.comment()
                );

        validateComment(
                request.decision(),
                comment
        );

        completeApprovalStep(
                approvalStep,
                currentUser,
                request.decision(),
                comment
        );

        applyDocumentDecision(
                document,
                approvalStep,
                request.decision()
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
                documentDecisionRepository
                        .save(
                                record
                        );

        documentAuditService
                .recordUserEvent(
                        document,
                        resolveAuditEventType(
                                request.decision()
                        ),
                        resolveAuditDescription(
                                request.decision()
                        ),
                        comment
                );

        return new DocumentDecisionResponse(
                savedRecord.getId(),

                document.getId(),

                savedRecord
                        .getDecision()
                        .name(),

                document
                        .getStatus()
                        .name(),

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
                                () ->
                                        new DocumentNotFoundException(
                                                "Документ с ID "
                                                        + documentId
                                                        + " не найден"
                                        )
                        );

        documentAccessService
                .requireAccess(
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

                                        record
                                                .getDecision()
                                                .name(),

                                        record
                                                .getDocument()
                                                .getStatus()
                                                .name(),

                                        record.getComment(),

                                        record
                                                .getUser()
                                                .getId(),

                                        record
                                                .getUser()
                                                .getFullName(),

                                        record.getCreatedAt()
                                )
                )
                .toList();
    }


    private DocumentApprovalStep resolveCurrentApprovalStep(
            Long documentId,
            AppUser currentUser
    ) {

        List<DocumentApprovalStep> inProgress =
                approvalStepRepository
                        .findByDocumentIdAndStatusIn(
                                documentId,
                                List.of(
                                        ApprovalStepStatus.IN_PROGRESS
                                )
                        );

        if (inProgress.isEmpty()) {

            boolean waitingForClaim =
                    !approvalStepRepository
                            .findByDocumentIdAndStatusIn(
                                    documentId,
                                    List.of(
                                            ApprovalStepStatus.PENDING
                                    )
                            )
                            .isEmpty();

            if (waitingForClaim) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Сначала возьмите документ в работу"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Для документа нет активного этапа согласования"
            );
        }

        DocumentApprovalStep step =
                inProgress.get(0);

        if (
                step.getAssignedUser() == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "У этапа согласования не назначен ответственный"
            );
        }

        if (
                !currentUser
                        .getId()
                        .equals(
                                step
                                        .getAssignedUser()
                                        .getId()
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Решение может принять только специалист, взявший документ в работу"
            );
        }

        String currentRole =
                currentUser
                        .getRole()
                        .getCode();

        if (
                !step.getRoleCode()
                        .equals(
                                currentRole
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Этап согласования предназначен для другой роли"
            );
        }

        return step;
    }


    private void completeApprovalStep(
            DocumentApprovalStep step,
            AppUser currentUser,
            DocumentDecision decision,
            String comment
    ) {

        ApprovalStepStatus targetStatus =
                switch (
                        decision
                ) {

                    case APPROVE ->
                            ApprovalStepStatus.APPROVED;

                    case RETURN_FOR_CORRECTION ->
                            ApprovalStepStatus.RETURNED;

                    case REJECT ->
                            ApprovalStepStatus.REJECTED;
                };

        step.setStatus(
                targetStatus
        );

        step.setComment(
                comment
        );

        step.setCompletedByUser(
                currentUser
        );

        step.setCompletedAt(
                LocalDateTime.now()
        );

        approvalStepRepository.save(
                step
        );
    }


    private void applyDocumentDecision(
            Document document,
            DocumentApprovalStep completedStep,
            DocumentDecision decision
    ) {

        if (
                decision
                        == DocumentDecision.RETURN_FOR_CORRECTION
        ) {

            cancelOtherActiveSteps(
                    document.getId(),
                    completedStep.getId()
            );

            document.setStatus(
                    DocumentStatus.RETURNED_FOR_CORRECTION
            );

            documentRepository.save(
                    document
            );

            return;
        }

        if (
                decision
                        == DocumentDecision.REJECT
        ) {

            cancelOtherActiveSteps(
                    document.getId(),
                    completedStep.getId()
            );

            document.setStatus(
                    DocumentStatus.REJECTED
            );

            documentRepository.save(
                    document
            );

            return;
        }

        boolean hasRemainingSteps =
                approvalStepRepository
                        .findByDocumentIdAndStatusIn(
                                document.getId(),
                                List.of(
                                        ApprovalStepStatus.PENDING,
                                        ApprovalStepStatus.IN_PROGRESS
                                )
                        )
                        .stream()
                        .anyMatch(
                                step ->
                                        !step
                                                .getId()
                                                .equals(
                                                        completedStep.getId()
                                                )
                        );

        if (hasRemainingSteps) {

            return;
        }

        /*
         * Последний этап согласования успешно завершён.
         *
         * Сначала фиксируем бизнес-статус APPROVED.
         * Затем SedPreparationService выполняет отдельные проверки:
         * реквизиты, связь с основным договором и активный маршрут.
         *
         * Если проверки успешны:
         * APPROVED -> READY_FOR_SED.
         *
         * Если проверка завершится ошибкой,
         * вся транзакция будет отменена.
         */
        document.setStatus(
                DocumentStatus.APPROVED
        );

        documentRepository.save(
                document
        );
    }


    private void cancelOtherActiveSteps(
            Long documentId,
            Long completedStepId
    ) {

        List<DocumentApprovalStep> otherSteps =
                approvalStepRepository
                        .findByDocumentIdAndStatusIn(
                                documentId,
                                List.of(
                                        ApprovalStepStatus.PENDING,
                                        ApprovalStepStatus.IN_PROGRESS
                                )
                        );

        LocalDateTime now =
                LocalDateTime.now();

        for (
                DocumentApprovalStep step
                : otherSteps
        ) {

            if (
                    step.getId()
                            .equals(
                                    completedStepId
                            )
            ) {

                continue;
            }

            step.setStatus(
                    ApprovalStepStatus.CANCELLED
            );

            step.setCompletedAt(
                    now
            );

            if (
                    step.getComment() == null
                            ||
                    step.getComment().isBlank()
            ) {

                step.setComment(
                        "Этап закрыт после завершения процесса согласования"
                );
            }
        }

        if (!otherSteps.isEmpty()) {

            approvalStepRepository
                    .saveAll(
                            otherSteps
                    );
        }
    }


    private void validateCurrentStatus(
            Document document
    ) {

        DocumentStatus status =
                document.getStatus();

        if (
                status != DocumentStatus.VERIFIED
                        &&
                status != DocumentStatus.LINKED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Решение специалиста доступно только для документа, находящегося на согласовании"
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
                        ||
                decision
                        == DocumentDecision.REJECT;

        if (
                commentRequired
                        &&
                (
                        comment == null
                                ||
                        comment.isBlank()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "При возврате на доработку или отклонении необходимо указать комментарий"
            );
        }

        if (
                comment != null
                        &&
                comment.length() > 2000
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Комментарий не должен превышать 2000 символов"
            );
        }
    }


    private DocumentAuditEventType resolveAuditEventType(
            DocumentDecision decision
    ) {

        return switch (
                decision
        ) {

            case APPROVE ->
                    DocumentAuditEventType.SPECIALIST_APPROVED;

            case RETURN_FOR_CORRECTION ->
                    DocumentAuditEventType.RETURNED_FOR_CORRECTION;

            case REJECT ->
                    DocumentAuditEventType.REJECTED;
        };
    }


    private String resolveAuditDescription(
            DocumentDecision decision
    ) {

        return switch (
                decision
        ) {

            case APPROVE ->
                    "Документ согласован специалистом";

            case RETURN_FOR_CORRECTION ->
                    "Документ возвращён на доработку";

            case REJECT ->
                    "Документ отклонён специалистом";
        };
    }


    private AppUser resolveCurrentUser() {

        Authentication authentication =
                getAuthentication();

        String login =
                authentication.getName();

        return appUserRepository
                .findByLogin(
                        login
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Текущий пользователь не найден"
                                )
                );
    }


    private void requireDecisionPermission() {

        if (
                hasAuthority(
                        "ROLE_LAWYER"
                )
                        ||
                hasAuthority(
                        "ROLE_CONTRACT_MANAGER"
                )
        ) {

            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Решение по обычному этапу согласования может принимать только назначенный специалист"
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
                        ||
                !authentication.isAuthenticated()
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