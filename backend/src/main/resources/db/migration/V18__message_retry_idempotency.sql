CREATE TABLE message_processing_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    document_id BIGINT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    attempt_number INT NOT NULL,
    processing_result VARCHAR(32) NOT NULL,
    error_reason VARCHAR(500) NULL,
    received_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX idx_processing_attempt_event
    ON message_processing_attempts (event_id);

CREATE INDEX idx_processing_attempt_consumer
    ON message_processing_attempts (consumer_name);

CREATE INDEX idx_processing_attempt_result
    ON message_processing_attempts (processing_result);


CREATE TABLE processed_message_ids (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_processed_message_event_consumer
        UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_processed_message_event
    ON processed_message_ids (event_id);