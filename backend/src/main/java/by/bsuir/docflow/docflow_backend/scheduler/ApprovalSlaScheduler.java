package by.bsuir.docflow.docflow_backend.scheduler;

import by.bsuir.docflow.docflow_backend.service.ApprovalSlaService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "docflow.approval-sla.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ApprovalSlaScheduler {

    private final ApprovalSlaService
            approvalSlaService;


    public ApprovalSlaScheduler(
            ApprovalSlaService approvalSlaService
    ) {

        this.approvalSlaService =
                approvalSlaService;
    }


    @Scheduled(
            cron =
                    "${docflow.approval-sla.cron:0 * * * * *}"
    )
    public void processApprovalSla() {

        approvalSlaService
                .processDeadlines();
    }
}