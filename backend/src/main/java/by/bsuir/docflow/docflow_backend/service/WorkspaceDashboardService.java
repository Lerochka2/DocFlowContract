package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.WorkspaceDashboardResponse;
import by.bsuir.docflow.docflow_backend.entity.ContractRiskAnalysis;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentApprovalStep;
import by.bsuir.docflow.docflow_backend.entity.enums.ApprovalStepStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.ContractExpirationStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentStatus;
import by.bsuir.docflow.docflow_backend.repository.ContractRiskAnalysisRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentApprovalStepRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkspaceDashboardService {

    private final DocumentRepository documentRepository;

    private final DocumentAccessService documentAccessService;

    private final ContractExpirationService contractExpirationService;

    private final DocumentApprovalStepRepository
            documentApprovalStepRepository;

    private final ContractRiskAnalysisRepository
            contractRiskAnalysisRepository;


    public WorkspaceDashboardService(
            DocumentRepository documentRepository,
            DocumentAccessService documentAccessService,
            ContractExpirationService contractExpirationService,
            DocumentApprovalStepRepository documentApprovalStepRepository,
            ContractRiskAnalysisRepository contractRiskAnalysisRepository
    ) {

        this.documentRepository =
                documentRepository;

        this.documentAccessService =
                documentAccessService;

        this.contractExpirationService =
                contractExpirationService;

        this.documentApprovalStepRepository =
                documentApprovalStepRepository;

        this.contractRiskAnalysisRepository =
                contractRiskAnalysisRepository;
    }


    @Transactional(readOnly = true)
    public WorkspaceDashboardResponse getDashboard() {

        List<Document> documents =
                documentAccessService
                        .filterAccessibleDocuments(
                                documentRepository.findAll()
                        );


        Set<Long> accessibleDocumentIds =
                documents
                        .stream()
                        .map(
                                Document::getId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        List<DocumentApprovalStep> approvalSteps =
                documentApprovalStepRepository
                        .findAll()
                        .stream()
                        .filter(step ->
                                step.getDocument() != null
                                        &&
                                accessibleDocumentIds.contains(
                                        step.getDocument()
                                                .getId()
                                )
                        )
                        .filter(step ->
                                step.getStatus()
                                        != ApprovalStepStatus.CANCELLED
                        )
                        .toList();


        LocalDateTime now =
                LocalDateTime.now();


        long approvalPending =
                countApprovalStatus(
                        approvalSteps,
                        ApprovalStepStatus.PENDING
                );


        long approvalInProgress =
                countApprovalStatus(
                        approvalSteps,
                        ApprovalStepStatus.IN_PROGRESS
                );


        long approvalApproved =
                countApprovalStatus(
                        approvalSteps,
                        ApprovalStepStatus.APPROVED
                );


        long approvalReturned =
                countApprovalStatus(
                        approvalSteps,
                        ApprovalStepStatus.RETURNED
                );


        long approvalRejected =
                countApprovalStatus(
                        approvalSteps,
                        ApprovalStepStatus.REJECTED
                );


        long approvalOverdue =
                approvalSteps
                        .stream()
                        .filter(
                                this::isActiveApproval
                        )
                        .filter(step ->
                                step.getDueAt() != null
                        )
                        .filter(step ->
                                !step.getDueAt()
                                        .isAfter(now)
                        )
                        .count();


        long approvalCompletedOnTime =
                approvalSteps
                        .stream()
                        .filter(
                                this::isCompletedApproval
                        )
                        .filter(step ->
                                step.getCompletedAt() != null
                                        &&
                                step.getDueAt() != null
                        )
                        .filter(step ->
                                !step.getCompletedAt()
                                        .isAfter(
                                                step.getDueAt()
                                        )
                        )
                        .count();


        long approvalCompletedLate =
                approvalSteps
                        .stream()
                        .filter(
                                this::isCompletedApproval
                        )
                        .filter(step ->
                                step.getCompletedAt() != null
                                        &&
                                step.getDueAt() != null
                        )
                        .filter(step ->
                                step.getCompletedAt()
                                        .isAfter(
                                                step.getDueAt()
                                        )
                        )
                        .count();


        long slaMeasured =
                approvalCompletedOnTime
                        +
                approvalCompletedLate;


        int approvalSlaCompliancePercent =
                slaMeasured == 0
                        ? 0
                        : (int) Math.round(
                                approvalCompletedOnTime
                                        * 100.0
                                        /
                                slaMeasured
                        );


        double averageApprovalTimeHours =
                roundOneDecimal(
                        approvalSteps
                                .stream()
                                .filter(
                                        this::isCompletedApproval
                                )
                                .filter(step ->
                                        step.getCreatedAt() != null
                                                &&
                                        step.getCompletedAt() != null
                                )
                                .filter(step ->
                                        !step.getCompletedAt()
                                                .isBefore(
                                                        step.getCreatedAt()
                                                )
                                )
                                .mapToDouble(step ->
                                        Duration
                                                .between(
                                                        step.getCreatedAt(),
                                                        step.getCompletedAt()
                                                )
                                                .toMinutes()
                                                /
                                        60.0
                                )
                                .average()
                                .orElse(0.0)
                );


        long completedApprovalDecisions =
                approvalApproved
                        +
                approvalReturned
                        +
                approvalRejected;


        int approvalReturnRatePercent =
                completedApprovalDecisions == 0
                        ? 0
                        : (int) Math.round(
                                approvalReturned
                                        * 100.0
                                        /
                                completedApprovalDecisions
                        );


        Map<Long, ContractRiskAnalysis> latestRiskByDocument =
                findLatestRiskAnalyses(
                        accessibleDocumentIds
                );


        long lowRiskContracts =
                countRiskLevel(
                        latestRiskByDocument,
                        "LOW"
                );


        long mediumRiskContracts =
                countRiskLevel(
                        latestRiskByDocument,
                        "MEDIUM"
                );


        long highRiskContracts =
                latestRiskByDocument
                        .values()
                        .stream()
                        .filter(analysis ->
                                analysis.getRiskLevel()
                                        != null
                        )
                        .filter(analysis -> {

                            String level =
                                    analysis
                                            .getRiskLevel()
                                            .toUpperCase();

                            return level.equals("HIGH")
                                    ||
                                    level.equals("CRITICAL")
                                    ||
                                    level.equals("VERY_HIGH");
                        })
                        .count();


        double averageRiskScore =
                roundOneDecimal(
                        latestRiskByDocument
                                .values()
                                .stream()
                                .filter(analysis ->
                                        analysis.getRiskScore()
                                                != null
                                )
                                .mapToInt(
                                        ContractRiskAnalysis::getRiskScore
                                )
                                .average()
                                .orElse(0.0)
                );


        return new WorkspaceDashboardResponse(

                documents.size(),

                countByStatus(
                        documents,
                        DocumentStatus.UPLOADED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.AI_PROCESSING
                ),

                countByStatus(
                        documents,
                        DocumentStatus.REVIEW_REQUIRED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.LINK_REQUIRED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.LINKED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.VERIFIED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.READY_FOR_SED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.SENT_TO_SED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.PROCESSING_FAILED
                ),

                countByStatus(
                        documents,
                        DocumentStatus.SED_ERROR
                ),

                countByExpirationStatus(
                        documents,
                        ContractExpirationStatus.EXPIRING_SOON
                ),

                countByExpirationStatus(
                        documents,
                        ContractExpirationStatus.EXPIRED
                ),

                approvalSteps.size(),

                approvalPending,

                approvalInProgress,

                approvalApproved,

                approvalReturned,

                approvalRejected,

                approvalOverdue,

                approvalCompletedOnTime,

                approvalCompletedLate,

                approvalSlaCompliancePercent,

                averageApprovalTimeHours,

                approvalReturnRatePercent,

                latestRiskByDocument.size(),

                lowRiskContracts,

                mediumRiskContracts,

                highRiskContracts,

                averageRiskScore
        );
    }


    private long countByStatus(
            List<Document> documents,
            DocumentStatus status
    ) {

        return documents
                .stream()
                .filter(document ->
                        document.getStatus()
                                == status
                )
                .count();
    }


    private long countByExpirationStatus(
            List<Document> documents,
            ContractExpirationStatus status
    ) {

        return documents
                .stream()
                .filter(document ->
                        contractExpirationService
                                .calculate(document)
                                .status()
                                == status
                )
                .count();
    }


    private long countApprovalStatus(
            List<DocumentApprovalStep> approvalSteps,
            ApprovalStepStatus status
    ) {

        return approvalSteps
                .stream()
                .filter(step ->
                        step.getStatus()
                                == status
                )
                .count();
    }


    private boolean isActiveApproval(
            DocumentApprovalStep step
    ) {

        return step.getStatus()
                == ApprovalStepStatus.PENDING
                ||
                step.getStatus()
                        == ApprovalStepStatus.IN_PROGRESS;
    }


    private boolean isCompletedApproval(
            DocumentApprovalStep step
    ) {

        return step.getStatus()
                == ApprovalStepStatus.APPROVED
                ||
                step.getStatus()
                        == ApprovalStepStatus.RETURNED
                ||
                step.getStatus()
                        == ApprovalStepStatus.REJECTED;
    }


    private Map<Long, ContractRiskAnalysis>
    findLatestRiskAnalyses(
            Set<Long> accessibleDocumentIds
    ) {

        Map<Long, ContractRiskAnalysis> latest =
                new HashMap<>();


        for (
                ContractRiskAnalysis analysis
                :
                contractRiskAnalysisRepository.findAll()
        ) {

            if (analysis.getDocument() == null) {
                continue;
            }


            Long documentId =
                    analysis
                            .getDocument()
                            .getId();


            if (
                    !accessibleDocumentIds.contains(
                            documentId
                    )
            ) {
                continue;
            }


            ContractRiskAnalysis current =
                    latest.get(
                            documentId
                    );


            if (
                    current == null
                            ||
                    isNewerRiskAnalysis(
                            analysis,
                            current
                    )
            ) {

                latest.put(
                        documentId,
                        analysis
                );
            }
        }


        return latest;
    }


    private boolean isNewerRiskAnalysis(
            ContractRiskAnalysis candidate,
            ContractRiskAnalysis current
    ) {

        if (
                candidate.getAnalyzedAt() != null
                        &&
                current.getAnalyzedAt() != null
        ) {

            int comparison =
                    candidate
                            .getAnalyzedAt()
                            .compareTo(
                                    current.getAnalyzedAt()
                            );


            if (comparison != 0) {
                return comparison > 0;
            }
        }


        if (
                candidate.getId() == null
                        ||
                current.getId() == null
        ) {
            return false;
        }


        return candidate
                .getId()
                >
                current
                        .getId();
    }


    private long countRiskLevel(
            Map<Long, ContractRiskAnalysis> analyses,
            String riskLevel
    ) {

        return analyses
                .values()
                .stream()
                .filter(analysis ->
                        analysis.getRiskLevel()
                                != null
                )
                .filter(analysis ->
                        riskLevel.equalsIgnoreCase(
                                analysis.getRiskLevel()
                        )
                )
                .count();
    }


    private double roundOneDecimal(
            double value
    ) {

        return Math.round(
                value * 10.0
        )
                /
                10.0;
    }
}