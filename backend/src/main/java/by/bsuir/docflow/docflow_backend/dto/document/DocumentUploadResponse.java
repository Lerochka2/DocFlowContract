package by.bsuir.docflow.docflow_backend.dto.document;

public class DocumentUploadResponse {

    private final Long documentId;
    private final Long fileId;
    private final String originalName;
    private final String status;
    private final boolean duplicateDetected;
    private final String sha256Checksum;

    public DocumentUploadResponse(
            Long documentId,
            Long fileId,
            String originalName,
            String status,
            boolean duplicateDetected,
            String sha256Checksum
    ) {
        this.documentId = documentId;
        this.fileId = fileId;
        this.originalName = originalName;
        this.status = status;
        this.duplicateDetected = duplicateDetected;
        this.sha256Checksum = sha256Checksum;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStatus() {
        return status;
    }

    public boolean isDuplicateDetected() {
        return duplicateDetected;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }
}