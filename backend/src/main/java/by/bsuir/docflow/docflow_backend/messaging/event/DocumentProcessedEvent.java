package by.bsuir.docflow.docflow_backend.messaging.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record DocumentProcessedEvent(
        String eventId,
        String eventType,
        String sourceEventId,
        Long documentId,
        String originalName,
        String storagePath,
        String documentType,
        String processingStatus,
        String processedAt
) {

    public static DocumentProcessedEvent create(
            DocumentUploadedEvent source,
            String documentType
    ) {
        return new DocumentProcessedEvent(
                UUID.randomUUID().toString(),
                "DOCUMENT_PROCESSED",
                source.eventId(),
                source.documentId(),
                source.originalName(),
                source.storagePath(),
                documentType,
                "PROCESSED",
                OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }
}