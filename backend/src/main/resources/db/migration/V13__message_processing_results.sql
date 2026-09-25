CREATE TABLE message_processing_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    source_event_id VARCHAR(64) NULL,
    event_type VARCHAR(64) NOT NULL,
    document_id BIGINT NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processing_status VARCHAR(50) NOT NULL,
    processing_time_ms BIGINT NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX idx_message_processing_event_id
    ON message_processing_results (event_id);

CREATE INDEX idx_message_processing_document_id
    ON message_processing_results (document_id);