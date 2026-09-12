package by.bsuir.docflow.docflow_backend.dto;

public record ContractCandidateResponse(
        Long contractDocumentId,
        String documentNumber,
        String documentDate,
        String subject,
        String status
) {
}