CREATE TABLE contract_risk_analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,

    risk_level VARCHAR(20) NOT NULL,
    risk_score INT NOT NULL,

    summary VARCHAR(1000) NULL,
    analyzer_version VARCHAR(100) NOT NULL,

    analyzed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_contract_risk_analyses
        PRIMARY KEY (id),

    CONSTRAINT fk_contract_risk_analyses_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE
);

CREATE TABLE contract_risk_findings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,

    risk_code VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    risk_weight INT NOT NULL,

    evidence TEXT NULL,
    explanation TEXT NULL,
    recommendation TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_contract_risk_findings
        PRIMARY KEY (id),

    CONSTRAINT fk_contract_risk_findings_analysis
        FOREIGN KEY (analysis_id)
        REFERENCES contract_risk_analyses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_contract_risk_analyses_document
    ON contract_risk_analyses(document_id);

CREATE INDEX idx_contract_risk_analyses_analyzed_at
    ON contract_risk_analyses(analyzed_at);

CREATE INDEX idx_contract_risk_findings_analysis
    ON contract_risk_findings(analysis_id);

CREATE INDEX idx_contract_risk_findings_code
    ON contract_risk_findings(risk_code);

CREATE INDEX idx_contract_risk_findings_severity
    ON contract_risk_findings(severity);