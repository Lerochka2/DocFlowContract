package by.bsuir.docflow.docflow_backend.dto.approval;

import java.time.LocalDateTime;

public record ApprovalStepResponse(

        Long stepId,

        Long documentId,

        String documentNumber,

        String documentType,

        String documentStatus,

        Integer stepOrder,

        String roleCode,

        String status,

        String routingReason,

        Long assignedUserId,

        String assignedUserFullName,

        String comment,

        LocalDateTime dueAt,

        LocalDateTime startedAt,

        LocalDateTime completedAt,

        Long completedByUserId,

        String completedByUserFullName,

        LocalDateTime createdAt

) {
}