package by.bsuir.docflow.docflow_backend.messaging.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record DocumentUploadedEvent(
        String eventId,
        String eventType,
        Long documentId,
        String originalName,
        String storagePath,
        String status,
        String createdAt
) {

    public static DocumentUploadedEvent create(
            Long documentId,
            String originalName,
            String storagePath,
            String status
    ) {
        return new DocumentUploadedEvent(
                UUID.randomUUID().toString(),
                "DOCUMENT_UPLOADED",
                documentId,
                originalName,
                storagePath,
                status,
                OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }
}