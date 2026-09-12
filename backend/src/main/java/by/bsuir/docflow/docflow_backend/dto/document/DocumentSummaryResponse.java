package by.bsuir.docflow.docflow_backend.dto.document;

import java.time.LocalDateTime;

public class DocumentSummaryResponse {

    private final Long id;
    private final String originalName;
    private final String documentType;
    private final String status;
    private final String documentNumber;
    private final LocalDateTime createdAt;
    private final String createdBy;

    public DocumentSummaryResponse(
            Long id,
            String originalName,
            String documentType,
            String status,
            String documentNumber,
            LocalDateTime createdAt,
            String createdBy
    ) {
        this.id = id;
        this.originalName = originalName;
        this.documentType = documentType;
        this.status = status;
        this.documentNumber = documentNumber;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getStatus() {
        return status;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}