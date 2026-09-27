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

        long sedError,

        long expiringSoonContracts,

        long expiredContracts,

        long approvalCycles,

        long approvalPending,

        long approvalInProgress,

        long approvalApproved,

        long approvalReturned,

        long approvalRejected,

        long approvalOverdue,

        long approvalCompletedOnTime,

        long approvalCompletedLate,

        int approvalSlaCompliancePercent,

        double averageApprovalTimeHours,

        int approvalReturnRatePercent,

        long riskAnalyzedDocuments,

        long lowRiskContracts,

        long mediumRiskContracts,

        long highRiskContracts,

        double averageRiskScore

) {
}