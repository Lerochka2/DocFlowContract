package by.bsuir.docflow.docflow_backend.entity;

import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_approval_steps")
public class DocumentApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "document_id",
            nullable = false
    )
    private Document document;

    @Column(
            name = "step_order",
            nullable = false
    )
    private Integer stepOrder;

    @Column(
            name = "role_code",
            nullable = false,
            length = 50
    )
    private String roleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private AppUser assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private ApprovalStepStatus status =
            ApprovalStepStatus.PENDING;

    @Column(
            name = "routing_reason",
            length = 500
    )
    private String routingReason;

    @Column(
            name = "comment",
            length = 2000
    )
    private String comment;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private AppUser completedByUser;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    public DocumentApprovalStep() {
    }


    @PrePersist
    public void prePersist() {

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status =
                    ApprovalStepStatus.PENDING;
        }
    }


    @PreUpdate
    public void preUpdate() {

        updatedAt =
                LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }


    public Document getDocument() {
        return document;
    }


    public void setDocument(
            Document document
    ) {
        this.document = document;
    }


    public Integer getStepOrder() {
        return stepOrder;
    }


    public void setStepOrder(
            Integer stepOrder
    ) {
        this.stepOrder = stepOrder;
    }


    public String getRoleCode() {
        return roleCode;
    }


    public void setRoleCode(
            String roleCode
    ) {
        this.roleCode = roleCode;
    }


    public AppUser getAssignedUser() {
        return assignedUser;
    }


    public void setAssignedUser(
            AppUser assignedUser
    ) {
        this.assignedUser =
                assignedUser;
    }


    public ApprovalStepStatus getStatus() {
        return status;
    }


    public void setStatus(
            ApprovalStepStatus status
    ) {
        this.status = status;
    }


    public String getRoutingReason() {
        return routingReason;
    }


    public void setRoutingReason(
            String routingReason
    ) {
        this.routingReason =
                routingReason;
    }


    public String getComment() {
        return comment;
    }


    public void setComment(
            String comment
    ) {
        this.comment = comment;
    }


    public LocalDateTime getDueAt() {
        return dueAt;
    }


    public void setDueAt(
            LocalDateTime dueAt
    ) {
        this.dueAt = dueAt;
    }


    public LocalDateTime getStartedAt() {
        return startedAt;
    }


    public void setStartedAt(
            LocalDateTime startedAt
    ) {
        this.startedAt = startedAt;
    }


    public LocalDateTime getCompletedAt() {
        return completedAt;
    }


    public void setCompletedAt(
            LocalDateTime completedAt
    ) {
        this.completedAt =
                completedAt;
    }


    public AppUser getCompletedByUser() {
        return completedByUser;
    }


    public void setCompletedByUser(
            AppUser completedByUser
    ) {
        this.completedByUser =
                completedByUser;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}