package by.bsuir.docflow.docflow_backend.dto;

import by.bsuir.docflow.docflow_backend.dto.ai.AiRiskFindingResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ContractRiskAnalysisResponse {

    private Long analysisId;

    private String riskLevel;

    private Integer riskScore;

    private Integer riskCount;

    private List<AiRiskFindingResponse> risks =
            new ArrayList<>();

    private String summary;

    private String analyzerVersion;

    private LocalDateTime analyzedAt;

    public ContractRiskAnalysisResponse() {
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(
            Long analysisId
    ) {
        this.analysisId = analysisId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(
            String riskLevel
    ) {
        this.riskLevel = riskLevel;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(
            Integer riskScore
    ) {
        this.riskScore = riskScore;
    }

    public Integer getRiskCount() {
        return riskCount;
    }

    public void setRiskCount(
            Integer riskCount
    ) {
        this.riskCount = riskCount;
    }

    public List<AiRiskFindingResponse> getRisks() {
        return risks;
    }

    public void setRisks(
            List<AiRiskFindingResponse> risks
    ) {
        this.risks =
                risks != null
                        ? risks
                        : new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(
            String summary
    ) {
        this.summary = summary;
    }

    public String getAnalyzerVersion() {
        return analyzerVersion;
    }

    public void setAnalyzerVersion(
            String analyzerVersion
    ) {
        this.analyzerVersion = analyzerVersion;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(
            LocalDateTime analyzedAt
    ) {
        this.analyzedAt = analyzedAt;
    }
}