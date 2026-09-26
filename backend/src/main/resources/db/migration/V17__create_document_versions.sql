CREATE TABLE document_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,

    document_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,

    version_number INT NOT NULL,

    created_by_user_id BIGINT NULL,

    change_note VARCHAR(1000) NULL,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uq_document_versions_document_version
        UNIQUE (document_id, version_number),

    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id)
        REFERENCES documents (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_document_versions_file
        FOREIGN KEY (file_id)
        REFERENCES stored_files (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_document_versions_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_document_versions_document
    ON document_versions (document_id);

CREATE INDEX idx_document_versions_file
    ON document_versions (file_id);


INSERT INTO document_versions (
    document_id,
    file_id,
    version_number,
    created_by_user_id,
    change_note,
    created_at
)
SELECT
    d.id,
    d.file_id,
    1,
    d.created_by,
    'Исходная версия',
    COALESCE(
        d.created_at,
        CURRENT_TIMESTAMP(6)
    )
FROM documents d
WHERE d.file_id IS NOT NULL;


ALTER TABLE document_approval_steps
    ADD COLUMN document_version_id BIGINT NULL
    AFTER document_id;


CREATE INDEX idx_document_approval_steps_version
    ON document_approval_steps (
        document_version_id
    );


ALTER TABLE document_approval_steps
    ADD CONSTRAINT fk_document_approval_steps_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions (id)
        ON DELETE SET NULL;


UPDATE document_approval_steps das
JOIN document_versions dv
    ON dv.document_id = das.document_id
    AND dv.version_number = 1
SET das.document_version_id = dv.id
WHERE das.document_version_id IS NULL;