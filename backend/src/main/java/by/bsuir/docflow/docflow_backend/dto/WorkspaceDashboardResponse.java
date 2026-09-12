package by.bsuir.docflow.docflow_backend.dto;

public record WorkspaceDashboardResponse(
        long totalDocuments,
        long uploaded,
        long aiProcessing,
        long reviewRequired,
        long linkRequired,
        long linked,
        long verified,
        long readyForSed,
        long sentToSed,
        long processingFailed,
        long sedError
) {
}