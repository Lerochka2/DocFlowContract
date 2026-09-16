package by.bsuir.docflow.docflow_backend.entity;

import by.bsuir.docflow.docflow_backend.entity.enums.AuditActorType;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentAuditEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_audit_events")
public class DocumentAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false
    )
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id"
    )
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "actor_type",
            nullable = false,
            length = 20
    )
    private AuditActorType actorType;

    @Column(
            name = "actor_name",
            nullable = false,
            length = 255
    )
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            length = 80
    )
    private DocumentAuditEventType eventType;

    @Column(
            name = "description",
            nullable = false,
            length = 1000
    )
    private String description;

    @Lob
    @Column(
            name = "details",
            columnDefinition = "TEXT"
    )
    private String details;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(
            Document document
    ) {
        this.document = document;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(
            AppUser user
    ) {
        this.user = user;
    }

    public AuditActorType getActorType() {
        return actorType;
    }

    public void setActorType(
            AuditActorType actorType
    ) {
        this.actorType = actorType;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(
            String actorName
    ) {
        this.actorName = actorName;
    }

    public DocumentAuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(
            DocumentAuditEventType eventType
    ) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(
            String details
    ) {
        this.details = details;
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