CREATE TABLE document_links (
    id BIGINT NOT NULL AUTO_INCREMENT,
    child_document_id BIGINT NOT NULL,
    contract_document_id BIGINT NOT NULL,
    linked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_links
        PRIMARY KEY (id),

    CONSTRAINT uq_document_links_child
        UNIQUE (child_document_id),

    CONSTRAINT fk_document_links_child
        FOREIGN KEY (child_document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_document_links_contract
        FOREIGN KEY (contract_document_id)
        REFERENCES documents(id),

    CONSTRAINT chk_document_links_different
        CHECK (child_document_id <> contract_document_id)
);

CREATE INDEX idx_document_links_contract
    ON document_links(contract_document_id);

CREATE INDEX idx_document_links_created_at
    ON document_links(created_at);