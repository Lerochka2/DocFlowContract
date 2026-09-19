package by.bsuir.docflow.docflow_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_risk_analyses")
public class ContractRiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false
    )
    private Document document;

    @Column(
            name = "risk_level",
            nullable = false,
            length = 20
    )
    private String riskLevel;

    @Column(
            name = "risk_score",
            nullable = false
    )
    private Integer riskScore;

    @Column(
            name = "summary",
            length = 1000
    )
    private String summary;

    @Column(
            name = "analyzer_version",
            nullable = false,
            length = 100
    )
    private String analyzerVersion;

    @Column(
            name = "analyzed_at",
            nullable = false
    )
    private LocalDateTime analyzedAt;

    @OneToMany(
            mappedBy = "analysis",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<ContractRiskFinding> findings =
            new ArrayList<>();

    @PrePersist
    public void prePersist() {

        if (analyzedAt == null) {
            analyzedAt =
                    LocalDateTime.now();
        }
    }

    public void addFinding(
            ContractRiskFinding finding
    ) {

        if (finding == null) {
            return;
        }

        finding.setAnalysis(this);

        findings.add(
                finding
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(
            Document document
    ) {
        this.document = document;
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

    public List<ContractRiskFinding> getFindings() {
        return findings;
    }

    public void setFindings(
            List<ContractRiskFinding> findings
    ) {

        this.findings.clear();

        if (findings == null) {
            return;
        }

        for (
                ContractRiskFinding finding
                : findings
        ) {
            addFinding(
                    finding
            );
        }
    }
}