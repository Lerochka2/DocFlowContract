package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentApprovalStep;
import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentApprovalStepRepository
        extends JpaRepository<
                DocumentApprovalStep,
                Long
        > {

    List<DocumentApprovalStep>
    findByDocumentIdOrderByStepOrderAsc(
            Long documentId
    );


    boolean existsByDocumentId(
            Long documentId
    );


    Optional<DocumentApprovalStep>
    findFirstByDocumentIdAndStatusInOrderByStepOrderAsc(
            Long documentId,
            Collection<ApprovalStepStatus> statuses
    );


    List<DocumentApprovalStep>
    findByRoleCodeAndStatusInOrderByDueAtAscCreatedAtAsc(
            String roleCode,
            Collection<ApprovalStepStatus> statuses
    );


    List<DocumentApprovalStep>
    findByAssignedUser_IdAndStatusInOrderByDueAtAscCreatedAtAsc(
            Long userId,
            Collection<ApprovalStepStatus> statuses
    );


    List<DocumentApprovalStep>
    findByDocumentIdAndStatusIn(
            Long documentId,
            Collection<ApprovalStepStatus> statuses
    );


    List<DocumentApprovalStep>
    findByStatusInAndDueAtLessThanEqualOrderByDueAtAsc(
            Collection<ApprovalStepStatus> statuses,
            LocalDateTime dueAt
    );
}