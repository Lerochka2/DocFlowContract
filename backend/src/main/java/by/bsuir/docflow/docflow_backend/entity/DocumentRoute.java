package by.bsuir.docflow.docflow_backend.entity;

import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingTarget;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_routes")
public class DocumentRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false
    )
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            nullable = false,
            length = 50
    )
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "routing_target",
            nullable = false,
            length = 50
    )
    private RoutingTarget routingTarget;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "routing_status",
            nullable = false,
            length = 50
    )
    private RoutingStatus routingStatus;

    @Column(
            name = "routing_reason",
            length = 500
    )
    private String routingReason;

    @Column(
            name = "routed_at",
            nullable = false
    )
    private LocalDateTime routedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public DocumentRoute() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (routingStatus == null) {
            routingStatus = RoutingStatus.ROUTED;
        }

        if (routedAt == null) {
            routedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public RoutingTarget getRoutingTarget() {
        return routingTarget;
    }

    public void setRoutingTarget(
            RoutingTarget routingTarget
    ) {
        this.routingTarget = routingTarget;
    }

    public RoutingStatus getRoutingStatus() {
        return routingStatus;
    }

    public void setRoutingStatus(
            RoutingStatus routingStatus
    ) {
        this.routingStatus = routingStatus;
    }

    public String getRoutingReason() {
        return routingReason;
    }

    public void setRoutingReason(
            String routingReason
    ) {
        this.routingReason = routingReason;
    }

    public LocalDateTime getRoutedAt() {
        return routedAt;
    }

    public void setRoutedAt(
            LocalDateTime routedAt
    ) {
        this.routedAt = routedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}