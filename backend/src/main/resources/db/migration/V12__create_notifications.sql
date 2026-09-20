CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,
    document_id BIGINT NULL,

    notification_type VARCHAR(50) NOT NULL,

    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,

    route VARCHAR(500) NULL,
    action_label VARCHAR(150) NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,

    dedup_key VARCHAR(255) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notifications
        PRIMARY KEY (id),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notifications_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_notifications_user_dedup
        UNIQUE (user_id, dedup_key)
);

CREATE INDEX idx_notifications_user
    ON notifications(user_id);

CREATE INDEX idx_notifications_document
    ON notifications(document_id);

CREATE INDEX idx_notifications_user_read_created
    ON notifications(user_id, is_read, created_at);

CREATE INDEX idx_notifications_created_at
    ON notifications(created_at);
