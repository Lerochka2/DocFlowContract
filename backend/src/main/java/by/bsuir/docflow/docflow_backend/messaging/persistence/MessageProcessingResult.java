package by.bsuir.docflow.docflow_backend.messaging.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_processing_results")
public class MessageProcessingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "source_event_id", length = 64)
    private String sourceEventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "processing_status", nullable = false, length = 50)
    private String processingStatus;

    @Column(name = "processing_time_ms", nullable = false)
    private Long processingTimeMs;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected MessageProcessingResult() {
    }

    public MessageProcessingResult(
            String eventId,
            String sourceEventId,
            String eventType,
            Long documentId,
            String consumerName,
            String processingStatus,
            Long processingTimeMs,
            LocalDateTime processedAt
    ) {
        this.eventId = eventId;
        this.sourceEventId = sourceEventId;
        this.eventType = eventType;
        this.documentId = documentId;
        this.consumerName = consumerName;
        this.processingStatus = processingStatus;
        this.processingTimeMs = processingTimeMs;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}