package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.approval.ApprovalStepResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentApprovalStep;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.DocumentVersion;
import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingStatus;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentApprovalStepRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentVersionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalWorkflowService {

    private final DocumentApprovalStepRepository approvalStepRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRouteRepository documentRouteRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final AppUserRepository appUserRepository;


    public ApprovalWorkflowService(
            DocumentApprovalStepRepository approvalStepRepository,
            DocumentRepository documentRepository,
            DocumentRouteRepository documentRouteRepository,
            DocumentVersionRepository documentVersionRepository,
            AppUserRepository appUserRepository
    ) {
        this.approvalStepRepository =
                approvalStepRepository;

        this.documentRepository =
                documentRepository;

        this.documentRouteRepository =
                documentRouteRepository;

        this.documentVersionRepository =
                documentVersionRepository;

        this.appUserRepository =
                appUserRepository;
    }


    @Transactional
    public List<ApprovalStepResponse> initializeFromCurrentRoute(
            Long documentId
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Документ с ID "
                                                        + documentId
                                                        + " не найден"
                                        )
                        );

        if (
                document.getStatus()
                        != DocumentStatus.VERIFIED
                        &&
                document.getStatus()
                        != DocumentStatus.LINKED
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Согласование можно сформировать только "
                            + "для проверенного или привязанного документа"
            );
        }

        DocumentRoute route =
                documentRouteRepository
                        .findFirstByDocumentIdAndRoutingStatusOrderByCreatedAtDesc(
                                documentId,
                                RoutingStatus.ROUTED
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.CONFLICT,
                                                "Для документа отсутствует активный маршрут"
                                        )
                        );

        ensureForRoute(
                document,
                route
        );

        return approvalStepRepository
                .findByDocumentIdOrderByStepOrderAsc(
                        documentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public void ensureForRoute(
            Document document,
            DocumentRoute route
    ) {

        if (
                document == null
                        ||
                document.getId() == null
                        ||
                route == null
                        ||
                route.getRoutingTarget() == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно сформировать этап согласования"
            );
        }

        if (
                document.getStatus()
                        != DocumentStatus.VERIFIED
                        &&
                document.getStatus()
                        != DocumentStatus.LINKED
        ) {

            return;
        }

        String requiredRole =
                route.getRoutingTarget()
                        .name();

        List<DocumentApprovalStep> existing =
                approvalStepRepository
                        .findByDocumentIdOrderByStepOrderAsc(
                                document.getId()
                        );

        List<DocumentApprovalStep> active =
                existing
                        .stream()
                        .filter(
                                step ->
                                        step.getStatus()
                                                == ApprovalStepStatus.PENDING
                                                ||
                                        step.getStatus()
                                                == ApprovalStepStatus.IN_PROGRESS
                        )
                        .toList();

        for (
                DocumentApprovalStep step
                : active
        ) {

            if (
                    requiredRole.equals(
                            step.getRoleCode()
                    )
            ) {

                return;
            }
        }

        LocalDateTime now =
                LocalDateTime.now();

        for (
                DocumentApprovalStep step
                : active
        ) {

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
                        "Этап отменён из-за изменения маршрута документа"
                );
            }
        }

        if (!active.isEmpty()) {

            approvalStepRepository.saveAll(
                    active
            );
        }

        int nextOrder =
                existing
                        .stream()
                        .map(
                                DocumentApprovalStep::getStepOrder
                        )
                        .filter(
                                value ->
                                        value != null
                        )
                        .max(Integer::compareTo)
                        .orElse(0)
                        + 1;

        DocumentVersion currentVersion =
                resolveCurrentVersion(
                        document
                );

        DocumentApprovalStep step =
                new DocumentApprovalStep();

        step.setDocument(
                document
        );

        step.setDocumentVersion(
                currentVersion
        );

        step.setStepOrder(
                nextOrder
        );

        step.setRoleCode(
                requiredRole
        );

        step.setStatus(
                ApprovalStepStatus.PENDING
        );

        step.setRoutingReason(
                route.getRoutingReason()
        );

        step.setDueAt(
                now.plusDays(2)
        );

        approvalStepRepository.save(
                step
        );
    }


    @Transactional(readOnly = true)
    public List<ApprovalStepResponse> getDocumentSteps(
            Long documentId
    ) {

        if (!documentRepository.existsById(documentId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Документ с ID "
                            + documentId
                            + " не найден"
            );
        }

        return approvalStepRepository
                .findByDocumentIdOrderByStepOrderAsc(
                        documentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ApprovalStepResponse> getMyQueue() {

        AppUser currentUser =
                resolveCurrentUser();

        String roleCode =
                currentUser
                        .getRole()
                        .getCode();

        if (
                !"LAWYER".equals(roleCode)
                        &&
                !"CONTRACT_MANAGER".equals(roleCode)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Очередь согласования доступна только специалистам"
            );
        }

        List<DocumentApprovalStep> available =
                approvalStepRepository
                        .findByRoleCodeAndStatusInOrderByDueAtAscCreatedAtAsc(
                                roleCode,
                                List.of(
                                        ApprovalStepStatus.PENDING
                                )
                        );

        List<DocumentApprovalStep> assigned =
                approvalStepRepository
                        .findByAssignedUser_IdAndStatusInOrderByDueAtAscCreatedAtAsc(
                                currentUser.getId(),
                                List.of(
                                        ApprovalStepStatus.IN_PROGRESS
                                )
                        );

        Map<Long, DocumentApprovalStep> unique =
                new LinkedHashMap<>();

        for (
                DocumentApprovalStep step
                : assigned
        ) {

            unique.put(
                    step.getId(),
                    step
            );
        }

        for (
                DocumentApprovalStep step
                : available
        ) {

            if (
                    step.getAssignedUser()
                            == null
            ) {

                unique.put(
                        step.getId(),
                        step
                );
            }
        }

        return new ArrayList<>(
                unique.values()
        )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public ApprovalStepResponse claim(
            Long stepId
    ) {

        AppUser currentUser =
                resolveCurrentUser();

        DocumentApprovalStep step =
                approvalStepRepository
                        .findById(stepId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Этап согласования не найден"
                                        )
                        );

        String currentRole =
                currentUser
                        .getRole()
                        .getCode();

        if (
                !step.getRoleCode()
                        .equals(currentRole)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Этот документ предназначен для другой роли"
            );
        }

        if (
                step.getStatus()
                        == ApprovalStepStatus.IN_PROGRESS
                        &&
                step.getAssignedUser() != null
                        &&
                currentUser.getId()
                        .equals(
                                step.getAssignedUser()
                                        .getId()
                        )
        ) {

            return toResponse(
                    step
            );
        }

        if (
                step.getStatus()
                        != ApprovalStepStatus.PENDING
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже взят в работу или этап завершён"
            );
        }

        if (
                step.getAssignedUser()
                        != null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Документ уже назначен другому специалисту"
            );
        }

        step.setAssignedUser(
                currentUser
        );

        step.setStatus(
                ApprovalStepStatus.IN_PROGRESS
        );

        step.setStartedAt(
                LocalDateTime.now()
        );

        DocumentApprovalStep saved =
                approvalStepRepository.save(
                        step
                );

        return toResponse(
                saved
        );
    }


    private DocumentVersion resolveCurrentVersion(
            Document document
    ) {

        if (
                document.getFile() == null
                        ||
                document.getFile().getId() == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "У документа отсутствует текущий файл"
            );
        }

        Long documentId =
                document.getId();

        Long fileId =
                document.getFile().getId();

        return documentVersionRepository
                .findByDocumentIdAndFileId(
                        documentId,
                        fileId
                )
                .orElseGet(
                        () -> {

                            int nextVersionNumber =
                                    documentVersionRepository
                                            .findFirstByDocumentIdOrderByVersionNumberDesc(
                                                    documentId
                                            )
                                            .map(
                                                    latest ->
                                                            latest.getVersionNumber()
                                                                    + 1
                                            )
                                            .orElse(1);

                            DocumentVersion version =
                                    new DocumentVersion();

                            version.setDocument(
                                    document
                            );

                            version.setFile(
                                    document.getFile()
                            );

                            version.setVersionNumber(
                                    nextVersionNumber
                            );

                            version.setCreatedBy(
                                    document.getCreatedBy()
                            );

                            version.setChangeNote(
                                    nextVersionNumber == 1
                                            ? "Исходная версия"
                                            : "Версия зарегистрирована системой"
                            );

                            return documentVersionRepository.save(
                                    version
                            );
                        }
                );
    }


    private AppUser resolveCurrentUser() {

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


    private ApprovalStepResponse toResponse(
            DocumentApprovalStep step
    ) {

        AppUser assigned =
                step.getAssignedUser();

        AppUser completedBy =
                step.getCompletedByUser();

        Document document =
                step.getDocument();

        return new ApprovalStepResponse(
                step.getId(),

                document.getId(),

                document.getDocumentNumber(),

                document.getDocumentType() == null
                        ? null
                        : document.getDocumentType()
                                .name(),

                document.getStatus() == null
                        ? null
                        : document.getStatus()
                                .name(),

                step.getStepOrder(),

                step.getRoleCode(),

                step.getStatus() == null
                        ? null
                        : step.getStatus()
                                .name(),

                step.getRoutingReason(),

                assigned == null
                        ? null
                        : assigned.getId(),

                assigned == null
                        ? null
                        : assigned.getFullName(),

                step.getComment(),

                step.getDueAt(),

                step.getStartedAt(),

                step.getCompletedAt(),

                completedBy == null
                        ? null
                        : completedBy.getId(),

                completedBy == null
                        ? null
                        : completedBy.getFullName(),

                step.getCreatedAt()
        );
    }
}