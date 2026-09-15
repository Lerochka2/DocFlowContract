CREATE TABLE document_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_comments
        PRIMARY KEY (id),

    CONSTRAINT fk_document_comments_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_document_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_document_comments_document
    ON document_comments(document_id);

CREATE INDEX idx_document_comments_user
    ON document_comments(user_id);

CREATE INDEX idx_document_comments_created_at
    ON document_comments(created_at);