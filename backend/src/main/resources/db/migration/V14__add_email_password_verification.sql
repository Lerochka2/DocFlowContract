ALTER TABLE users
    ADD COLUMN email VARCHAR(255) NULL AFTER full_name;

CREATE UNIQUE INDEX ux_users_email
    ON users(email);


CREATE TABLE password_verification_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,

    code_hash VARCHAR(255) NOT NULL,

    expires_at DATETIME NOT NULL,

    attempt_count INT NOT NULL DEFAULT 0,

    used BOOLEAN NOT NULL DEFAULT FALSE,

    verified_at DATETIME NULL,

    change_token_hash VARCHAR(255) NULL,

    change_token_expires_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_password_verification_codes
        PRIMARY KEY (id),

    CONSTRAINT fk_password_verification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_password_verification_user
    ON password_verification_codes(user_id);

CREATE INDEX idx_password_verification_expires
    ON password_verification_codes(expires_at);

CREATE INDEX idx_password_verification_created
    ON password_verification_codes(created_at);