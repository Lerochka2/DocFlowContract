package by.bsuir.docflow.docflow_backend.dto.ai;

import java.util.List;

public class AiRiskAnalysisResponse {

    private String riskLevel;
    private Integer riskScore;
    private Integer riskCount;
    private List<AiRiskFindingResponse> risks;
    private String summary;
    private String analyzerVersion;

    public AiRiskAnalysisResponse() {
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Integer getRiskCount() {
        return riskCount;
    }

    public void setRiskCount(Integer riskCount) {
        this.riskCount = riskCount;
    }

    public List<AiRiskFindingResponse> getRisks() {
        return risks;
    }

    public void setRisks(
            List<AiRiskFindingResponse> risks
    ) {
        this.risks = risks;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
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
}