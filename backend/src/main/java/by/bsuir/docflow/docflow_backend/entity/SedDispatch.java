package by.bsuir.docflow.docflow_backend.entity;

import by.bsuir.docflow.docflow_backend.entity.enums.SedDispatchStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sed_dispatches")
public class SedDispatch {

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
            name = "dispatch_status",
            nullable = false,
            length = 50
    )
    private SedDispatchStatus dispatchStatus;

    @Column(
            name = "external_document_id",
            length = 100
    )
    private String externalDocumentId;

    @Column(
            name = "response_message",
            length = 500
    )
    private String responseMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public SedDispatch() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
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

    public void setDocument(
            Document document
    ) {
        this.document = document;
    }

    public SedDispatchStatus getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(
            SedDispatchStatus dispatchStatus
    ) {
        this.dispatchStatus = dispatchStatus;
    }

    public String getExternalDocumentId() {
        return externalDocumentId;
    }

    public void setExternalDocumentId(
            String externalDocumentId
    ) {
        this.externalDocumentId = externalDocumentId;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(
            String responseMessage
    ) {
        this.responseMessage = responseMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(
            LocalDateTime sentAt
    ) {
        this.sentAt = sentAt;
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