CREATE TABLE document_approval_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,

    document_id BIGINT NOT NULL,

    step_order INT NOT NULL,

    role_code VARCHAR(50) NOT NULL,

    assigned_user_id BIGINT NULL,

    status VARCHAR(50) NOT NULL,

    routing_reason VARCHAR(500) NULL,

    comment VARCHAR(2000) NULL,

    due_at DATETIME NULL,

    started_at DATETIME NULL,

    completed_at DATETIME NULL,

    completed_by_user_id BIGINT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_approval_steps
        PRIMARY KEY (id),

    CONSTRAINT fk_document_approval_steps_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_document_approval_steps_assigned_user
        FOREIGN KEY (assigned_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_document_approval_steps_completed_by
        FOREIGN KEY (completed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT uq_document_approval_step_order
        UNIQUE (document_id, step_order)
);

CREATE INDEX idx_approval_steps_document
    ON document_approval_steps(document_id);

CREATE INDEX idx_approval_steps_status
    ON document_approval_steps(status);

CREATE INDEX idx_approval_steps_role
    ON document_approval_steps(role_code);

CREATE INDEX idx_approval_steps_assigned_user
    ON document_approval_steps(assigned_user_id);

CREATE INDEX idx_approval_steps_due_at
    ON document_approval_steps(due_at);


-- Создаём этап согласования для существующих документов,
-- которые уже были маршрутизированы до появления V16.

INSERT INTO document_approval_steps (
    document_id,
    step_order,
    role_code,
    assigned_user_id,
    status,
    routing_reason,
    due_at,
    created_at,
    updated_at
)
SELECT
    route.document_id,
    1,
    route.routing_target,
    NULL,
    'PENDING',
    route.routing_reason,
    DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 DAY),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM document_routes route
INNER JOIN (
    SELECT
        document_id,
        MAX(id) AS route_id
    FROM document_routes
    WHERE routing_status = 'ROUTED'
    GROUP BY document_id
) latest
    ON latest.route_id = route.id
INNER JOIN documents document
    ON document.id = route.document_id
WHERE document.status IN (
    'VERIFIED',
    'LINKED'
);