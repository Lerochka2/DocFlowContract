package by.bsuir.docflow.docflow_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_links")
public class DocumentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "child_document_id",
            nullable = false,
            unique = true
    )
    private Document childDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "contract_document_id",
            nullable = false
    )
    private Document contractDocument;

    @Column(
            name = "linked_at",
            nullable = false
    )
    private LocalDateTime linkedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public DocumentLink() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (linkedAt == null) {
            linkedAt = now;
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

    public Document getChildDocument() {
        return childDocument;
    }

    public void setChildDocument(
            Document childDocument
    ) {
        this.childDocument = childDocument;
    }

    public Document getContractDocument() {
        return contractDocument;
    }

    public void setContractDocument(
            Document contractDocument
    ) {
        this.contractDocument = contractDocument;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(
            LocalDateTime linkedAt
    ) {
        this.linkedAt = linkedAt;
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