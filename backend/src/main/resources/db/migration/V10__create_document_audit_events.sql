CREATE TABLE document_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    user_id BIGINT NULL,

    actor_type VARCHAR(20) NOT NULL,
    actor_name VARCHAR(255) NOT NULL,

    event_type VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    details TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_audit_events
        PRIMARY KEY (id),

    CONSTRAINT fk_document_audit_events_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_document_audit_events_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_document_audit_document
    ON document_audit_events(document_id);

CREATE INDEX idx_document_audit_user
    ON document_audit_events(user_id);

CREATE INDEX idx_document_audit_event_type
    ON document_audit_events(event_type);

CREATE INDEX idx_document_audit_created_at
    ON document_audit_events(created_at);