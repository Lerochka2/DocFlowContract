CREATE TABLE document_routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    routing_target VARCHAR(50) NOT NULL,
    routing_status VARCHAR(50) NOT NULL,
    routing_reason VARCHAR(500) NULL,
    routed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_routes
        PRIMARY KEY (id),

    CONSTRAINT fk_document_routes_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
);

CREATE INDEX idx_document_routes_document
    ON document_routes(document_id);

CREATE INDEX idx_document_routes_target
    ON document_routes(routing_target);

CREATE INDEX idx_document_routes_status
    ON document_routes(routing_status);

CREATE INDEX idx_document_routes_created_at
    ON document_routes(created_at);