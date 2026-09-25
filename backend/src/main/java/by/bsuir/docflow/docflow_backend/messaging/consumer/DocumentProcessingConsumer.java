package by.bsuir.docflow.docflow_backend.messaging.consumer;

import by.bsuir.docflow.docflow_backend.config.RabbitMqConfig;
import by.bsuir.docflow.docflow_backend.messaging.event.DocumentProcessedEvent;
import by.bsuir.docflow.docflow_backend.messaging.event.DocumentUploadedEvent;
import by.bsuir.docflow.docflow_backend.messaging.persistence.MessageProcessingResultService;
import by.bsuir.docflow.docflow_backend.messaging.producer.DocumentEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "docflow.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DocumentProcessingConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final DocumentEventProducer producer;
    private final MessageProcessingResultService resultService;

    @Value("${DOCFLOW_LAB_PROCESSING_DELAY_MS:0}")
    private long processingDelayMs;

    public DocumentProcessingConsumer(
            DocumentEventProducer producer,
            MessageProcessingResultService resultService
    ) {
        this.producer = producer;
        this.resultService = resultService;
    }

    @RabbitListener(
            queues = RabbitMqConfig.DOCUMENT_UPLOADED_QUEUE,
            concurrency = "${DOCFLOW_CONSUMER_CONCURRENCY:1}"
    )
    public void process(DocumentUploadedEvent event) {

        long startedAt = System.nanoTime();

        log.info(
                "CONSUMER 1 RECEIVED: eventId={}, documentId={}, file={}",
                event.eventId(),
                event.documentId(),
                event.originalName()
        );

        validate(event);

        simulateProcessing();

        String documentType =
                determineDocumentType(event.originalName());

        DocumentProcessedEvent processedEvent =
                DocumentProcessedEvent.create(
                        event,
                        documentType
                );

        producer.sendDocumentProcessed(processedEvent);

        long processingTimeMs =
                (System.nanoTime() - startedAt) / 1_000_000;

        resultService.save(
                event.eventId(),
                null,
                event.eventType(),
                event.documentId(),
                "Document Processing Service",
                "PROCESSED",
                processingTimeMs
        );

        log.info(
                "CONSUMER 1 PROCESSED: sourceEventId={}, documentId={}, documentType={}, status={}, processingTimeMs={}",
                event.eventId(),
                event.documentId(),
                documentType,
                processedEvent.processingStatus(),
                processingTimeMs
        );
    }

    private void validate(DocumentUploadedEvent event) {

        if (event.eventId() == null ||
                event.eventId().isBlank()) {
            throw new IllegalArgumentException(
                    "eventId is required"
            );
        }

        if (event.documentId() == null) {
            throw new IllegalArgumentException(
                    "documentId is required"
            );
        }

        if (event.originalName() == null ||
                event.originalName().isBlank()) {
            throw new IllegalArgumentException(
                    "originalName is required"
            );
        }
    }

    private void simulateProcessing() {

        if (processingDelayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Processing was interrupted",
                    e
            );
        }
    }

    private String determineDocumentType(String fileName) {

        String normalized =
                fileName.toLowerCase();

        if (normalized.contains("additional") ||
                normalized.contains("agreement")) {
            return "ADDITIONAL_AGREEMENT";
        }

        return "CONTRACT";
    }
}