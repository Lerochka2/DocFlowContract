package by.bsuir.docflow.docflow_backend.dto.version;

import java.time.LocalDateTime;

public record DocumentVersionResponse(
        Long versionId,
        Long documentId,
        Long fileId,
        Integer versionNumber,
        String fileName,
        String mimeType,
        Long sizeBytes,
        String sha256Checksum,
        Long createdByUserId,
        String createdByFullName,
        String changeNote,
        LocalDateTime createdAt,
        boolean currentVersion
) {
}