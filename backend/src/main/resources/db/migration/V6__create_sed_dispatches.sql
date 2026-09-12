CREATE TABLE sed_dispatches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    dispatch_status VARCHAR(50) NOT NULL,
    external_document_id VARCHAR(100) NULL,
    response_message VARCHAR(500) NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_sed_dispatches
        PRIMARY KEY (id),

    CONSTRAINT fk_sed_dispatches_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
);

CREATE INDEX idx_sed_dispatches_document
    ON sed_dispatches(document_id);

CREATE INDEX idx_sed_dispatches_status
    ON sed_dispatches(dispatch_status);

CREATE INDEX idx_sed_dispatches_created_at
    ON sed_dispatches(created_at);