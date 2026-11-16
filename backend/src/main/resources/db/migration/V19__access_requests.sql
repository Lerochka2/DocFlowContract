CREATE TABLE access_requests (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(255) NOT NULL,
 email VARCHAR(255) NOT NULL,
 department VARCHAR(255) NOT NULL,
 position VARCHAR(255) NOT NULL,
 comment VARCHAR(1000) NOT NULL DEFAULT '',
 status VARCHAR(16) NOT NULL DEFAULT 'NEW',
 admin_comment VARCHAR(1000),
 processed_by_user_id BIGINT,
 created_user_id BIGINT,
 created_at DATETIME(6) NOT NULL,
 processed_at DATETIME(6),
 pending_email VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN status = 'NEW' THEN email ELSE NULL END) STORED,
 UNIQUE KEY uk_access_pending_email (pending_email),
 KEY ix_access_created (created_at),
 CONSTRAINT fk_access_admin FOREIGN KEY (processed_by_user_id) REFERENCES users(id),
 CONSTRAINT fk_access_user FOREIGN KEY (created_user_id) REFERENCES users(id)
);
