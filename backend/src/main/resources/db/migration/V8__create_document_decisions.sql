CREATE TABLE document_decisions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    decision VARCHAR(50) NOT NULL,
    comment VARCHAR(2000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_decisions
        PRIMARY KEY (id),

    CONSTRAINT fk_document_decisions_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_document_decisions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_document_decisions_document
    ON document_decisions(document_id);

CREATE INDEX idx_document_decisions_user
    ON document_decisions(user_id);

CREATE INDEX idx_document_decisions_decision
    ON document_decisions(decision);

CREATE INDEX idx_document_decisions_created_at
    ON document_decisions(created_at);