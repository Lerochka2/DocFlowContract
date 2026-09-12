CREATE TABLE ai_processing_results (
    id BIGINT NOT NULL AUTO_INCREMENT,

    document_id BIGINT NOT NULL,

    predicted_type VARCHAR(50) NULL,

    confidence DECIMAL(6, 5) NULL,

    classification_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    review_required BOOLEAN NOT NULL DEFAULT TRUE,

    recognized_text LONGTEXT NULL,

    ocr_used BOOLEAN NOT NULL DEFAULT FALSE,

    model_version VARCHAR(100) NULL,

    probabilities JSON NULL,

    processing_status VARCHAR(50) NOT NULL,

    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ai_processing_results
        PRIMARY KEY (id),

    CONSTRAINT fk_ai_processing_results_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT chk_ai_processing_confidence
        CHECK (
            confidence IS NULL
            OR (confidence >= 0 AND confidence <= 1)
        )
);

CREATE INDEX idx_ai_processing_document
    ON ai_processing_results(document_id);

CREATE INDEX idx_ai_processing_status
    ON ai_processing_results(processing_status);

CREATE INDEX idx_ai_processing_created_at
    ON ai_processing_results(created_at);