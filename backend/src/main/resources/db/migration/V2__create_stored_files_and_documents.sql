CREATE TABLE stored_files (
    id BIGINT NOT NULL AUTO_INCREMENT,

    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,

    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,

    sha256_checksum CHAR(64) NOT NULL,

    encrypted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_stored_files
        PRIMARY KEY (id),

    CONSTRAINT uk_stored_files_stored_name
        UNIQUE (stored_name),

    CONSTRAINT chk_stored_files_size
        CHECK (size_bytes >= 0)
);


CREATE TABLE documents (
    id BIGINT NOT NULL AUTO_INCREMENT,

    file_id BIGINT NOT NULL,

    document_type VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',

    document_number VARCHAR(100) NULL,
    document_date DATE NULL,

    amount DECIMAL(19, 2) NULL,
    currency CHAR(3) NULL,

    subject VARCHAR(1000) NULL,

    valid_from DATE NULL,
    valid_to DATE NULL,

    referenced_contract_number VARCHAR(100) NULL,
    referenced_contract_date DATE NULL,

    created_by BIGINT NOT NULL,
    verified_by BIGINT NULL,
    verified_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_documents
        PRIMARY KEY (id),

    CONSTRAINT uk_documents_file
        UNIQUE (file_id),

    CONSTRAINT fk_documents_file
        FOREIGN KEY (file_id)
        REFERENCES stored_files(id),

    CONSTRAINT fk_documents_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_documents_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES users(id),

    CONSTRAINT chk_documents_amount
        CHECK (amount IS NULL OR amount >= 0)
);


CREATE INDEX idx_stored_files_sha256
    ON stored_files(sha256_checksum);


CREATE INDEX idx_documents_status
    ON documents(status);


CREATE INDEX idx_documents_type
    ON documents(document_type);


CREATE INDEX idx_documents_number
    ON documents(document_number);


CREATE INDEX idx_documents_date
    ON documents(document_date);


CREATE INDEX idx_documents_created_by
    ON documents(created_by);