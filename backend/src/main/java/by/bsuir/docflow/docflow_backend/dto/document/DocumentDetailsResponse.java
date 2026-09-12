package by.bsuir.docflow.docflow_backend.dto.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DocumentDetailsResponse {

    private final Long id;
    private final Long fileId;
    private final String originalName;
    private final String mimeType;
    private final Long sizeBytes;
    private final String sha256Checksum;

    private final String documentType;
    private final String status;
    private final String documentNumber;
    private final LocalDate documentDate;

    private final BigDecimal amount;
    private final String currency;
    private final String subject;

    private final LocalDate validFrom;
    private final LocalDate validTo;

    private final String referencedContractNumber;
    private final LocalDate referencedContractDate;

    private final Long createdById;
    private final String createdByName;

    private final Long verifiedById;
    private final String verifiedByName;
    private final LocalDateTime verifiedAt;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public DocumentDetailsResponse(
            Long id,
            Long fileId,
            String originalName,
            String mimeType,
            Long sizeBytes,
            String sha256Checksum,
            String documentType,
            String status,
            String documentNumber,
            LocalDate documentDate,
            BigDecimal amount,
            String currency,
            String subject,
            LocalDate validFrom,
            LocalDate validTo,
            String referencedContractNumber,
            LocalDate referencedContractDate,
            Long createdById,
            String createdByName,
            Long verifiedById,
            String verifiedByName,
            LocalDateTime verifiedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.fileId = fileId;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.sha256Checksum = sha256Checksum;
        this.documentType = documentType;
        this.status = status;
        this.documentNumber = documentNumber;
        this.documentDate = documentDate;
        this.amount = amount;
        this.currency = currency;
        this.subject = subject;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.referencedContractNumber = referencedContractNumber;
        this.referencedContractDate = referencedContractDate;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.verifiedById = verifiedById;
        this.verifiedByName = verifiedByName;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getFileId() { return fileId; }
    public String getOriginalName() { return originalName; }
    public String getMimeType() { return mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSha256Checksum() { return sha256Checksum; }
    public String getDocumentType() { return documentType; }
    public String getStatus() { return status; }
    public String getDocumentNumber() { return documentNumber; }
    public LocalDate getDocumentDate() { return documentDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getSubject() { return subject; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public String getReferencedContractNumber() { return referencedContractNumber; }
    public LocalDate getReferencedContractDate() { return referencedContractDate; }
    public Long getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public Long getVerifiedById() { return verifiedById; }
    public String getVerifiedByName() { return verifiedByName; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}