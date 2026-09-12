package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentLinkDetailsResponse(
        Long linkId,
        Long childDocumentId,
        String childDocumentType,
        String childDocumentStatus,
        Long contractDocumentId,
        String contractDocumentType,
        String contractDocumentStatus,
        LocalDateTime linkedAt
) {
}