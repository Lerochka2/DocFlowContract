package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentApprovalStep;
import by.bsuir.docflow.docflow_backend.entity.UserNotification;
import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.NotificationType;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentApprovalStepRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ApprovalSlaService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ApprovalSlaService.class
            );

    private static final List<ApprovalStepStatus>
            ACTIVE_STATUSES =
            List.of(
                    ApprovalStepStatus.PENDING,
                    ApprovalStepStatus.IN_PROGRESS
            );

    private static final DateTimeFormatter
            DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd.MM.yyyy HH:mm"
            );

    private final DocumentApprovalStepRepository
            approvalStepRepository;

    private final AppUserRepository
            appUserRepository;

    private final NotificationService
            notificationService;

    private final long reminderHours;


    public ApprovalSlaService(
            DocumentApprovalStepRepository approvalStepRepository,
            AppUserRepository appUserRepository,
            NotificationService notificationService,
            @Value(
                    "${docflow.approval-sla.reminder-hours:24}"
            )
            long reminderHours
    ) {

        this.approvalStepRepository =
                approvalStepRepository;

        this.appUserRepository =
                appUserRepository;

        this.notificationService =
                notificationService;

        this.reminderHours =
                reminderHours > 0
                        ? reminderHours
                        : 24;
    }


    @Transactional
    public void processDeadlines() {

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime reminderCutoff =
                now.plusHours(
                        reminderHours
                );

        List<DocumentApprovalStep> steps =
                approvalStepRepository
                        .findByStatusInAndDueAtLessThanEqualOrderByDueAtAsc(
                                ACTIVE_STATUSES,
                                reminderCutoff
                        );

        int dueSoonCreated = 0;
        int overdueCreated = 0;

        for (
                DocumentApprovalStep step
                : steps
        ) {

            if (step.getDueAt() == null) {
                continue;
            }

            if (
                    step.getDueAt()
                            .isAfter(now)
            ) {

                dueSoonCreated +=
                        notifyStep(
                                step,
                                NotificationType.APPROVAL_DUE_SOON
                        );

            } else {

                overdueCreated +=
                        notifyStep(
                                step,
                                NotificationType.APPROVAL_OVERDUE
                        );
            }
        }

        if (
                dueSoonCreated > 0
                        ||
                overdueCreated > 0
        ) {

            log.info(
                    "Approval SLA notifications created: dueSoon={}, overdue={}",
                    dueSoonCreated,
                    overdueCreated
            );
        }
    }


    private int notifyStep(
            DocumentApprovalStep step,
            NotificationType notificationType
    ) {

        List<AppUser> recipients =
                resolveRecipients(
                        step
                );

        if (recipients.isEmpty()) {
            return 0;
        }

        int createdCount = 0;

        Document document =
                step.getDocument();

        String documentLabel =
                buildDocumentLabel(
                        document
                );

        String deadline =
                step.getDueAt()
                        .format(
                                DEADLINE_FORMAT
                        );

        boolean overdue =
                notificationType
                        == NotificationType.APPROVAL_OVERDUE;

        String title =
                overdue
                        ? "Просрочено согласование документа"
                        : "Срок согласования скоро истекает";

        String message =
                overdue
                        ? "Срок согласования "
                            + documentLabel
                            + " истёк "
                            + deadline
                            + ". Требуется завершить обработку."
                        : "Срок согласования "
                            + documentLabel
                            + " истекает "
                            + deadline
                            + ". Примите решение до установленного срока.";

        for (
                AppUser recipient
                : recipients
        ) {

            String dedupKey =
                    notificationType.name()
                            + ":"
                            + step.getId();

            boolean assignedToRecipient =
                    step.getAssignedUser() != null
                            &&
                    step.getAssignedUser()
                            .getId()
                            .equals(
                                    recipient.getId()
                            );

            String route =
                    assignedToRecipient
                            ? "/documents/"
                                + document.getId()
                            : "/routing";

            String actionLabel =
                    assignedToRecipient
                            ? "Открыть документ"
                            : "Открыть очередь";

            UserNotification created =
                    notificationService
                            .createNotification(
                                    recipient,
                                    document,
                                    notificationType,
                                    title,
                                    message,
                                    route,
                                    actionLabel,
                                    dedupKey
                            );

            if (created != null) {
                createdCount++;
            }
        }

        return createdCount;
    }


    private List<AppUser> resolveRecipients(
            DocumentApprovalStep step
    ) {

        if (step.getAssignedUser() != null) {

            return List.of(
                    step.getAssignedUser()
            );
        }

        String roleCode =
                step.getRoleCode();

        if (
                roleCode == null
                        ||
                roleCode.isBlank()
        ) {

            return List.of();
        }

        return appUserRepository
                .findByRoleCodeAndEnabledTrue(
                        roleCode
                );
    }


    private String buildDocumentLabel(
            Document document
    ) {

        if (document == null) {
            return "документа";
        }

        String number =
                document.getDocumentNumber();

        if (
                number != null
                        &&
                !number.isBlank()
        ) {

            return "документа № "
                    + number.trim();
        }

        return "документа #"
                + document.getId();
    }
}