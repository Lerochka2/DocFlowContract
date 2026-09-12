package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDateTime;

public record DocumentLinkResponse(
        Long linkId,
        Long childDocumentId,
        String childDocumentType,
        Long contractDocumentId,
        String contractDocumentType,
        String documentStatus,
        String routingTarget,
        String routingStatus,
        LocalDateTime linkedAt
) {
}