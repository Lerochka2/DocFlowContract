package by.bsuir.docflow.docflow_backend.dto;

public record ManualReviewResponse(
        Long documentId,
        String documentType,
        String documentStatus,
        String routingTarget,
        String routingStatus
) {
}