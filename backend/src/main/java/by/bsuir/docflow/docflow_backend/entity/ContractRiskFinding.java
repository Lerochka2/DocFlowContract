package by.bsuir.docflow.docflow_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_risk_findings")
public class ContractRiskFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "analysis_id",
            nullable = false
    )
    private ContractRiskAnalysis analysis;

    @Column(
            name = "risk_code",
            nullable = false,
            length = 80
    )
    private String riskCode;

    @Column(
            name = "title",
            nullable = false,
            length = 255
    )
    private String title;

    @Column(
            name = "severity",
            nullable = false,
            length = 20
    )
    private String severity;

    @Column(
            name = "risk_weight",
            nullable = false
    )
    private Integer riskWeight;

    @Lob
    @Column(
            name = "evidence",
            columnDefinition = "TEXT"
    )
    private String evidence;

    @Lob
    @Column(
            name = "explanation",
            columnDefinition = "TEXT"
    )
    private String explanation;

    @Lob
    @Column(
            name = "recommendation",
            columnDefinition = "TEXT"
    )
    private String recommendation;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt =
                    LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public ContractRiskAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(
            ContractRiskAnalysis analysis
    ) {
        this.analysis = analysis;
    }

    public String getRiskCode() {
        return riskCode;
    }

    public void setRiskCode(
            String riskCode
    ) {
        this.riskCode = riskCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(
            String title
    ) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(
            String severity
    ) {
        this.severity = severity;
    }

    public Integer getRiskWeight() {
        return riskWeight;
    }

    public void setRiskWeight(
            Integer riskWeight
    ) {
        this.riskWeight = riskWeight;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(
            String evidence
    ) {
        this.evidence = evidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(
            String explanation
    ) {
        this.explanation = explanation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(
            String recommendation
    ) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}