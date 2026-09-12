package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record SedDispatchResponse(
        Long dispatchId,
        Long documentId,
        String documentType,
        String documentStatus,
        String dispatchStatus,
        String externalDocumentId,
        String responseMessage,
        LocalDateTime sentAt
) {
}