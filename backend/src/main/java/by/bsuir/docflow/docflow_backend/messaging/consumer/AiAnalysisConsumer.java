package by.bsuir.docflow.docflow_backend.messaging.consumer;

import by.bsuir.docflow.docflow_backend.config.RabbitMqConfig;
import by.bsuir.docflow.docflow_backend.messaging.event.DocumentProcessedEvent;
import by.bsuir.docflow.docflow_backend.messaging.persistence.MessageProcessingResultService;
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
public class AiAnalysisConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AiAnalysisConsumer.class);

    private final MessageProcessingResultService resultService;

    @Value("${DOCFLOW_LAB_PROCESSING_DELAY_MS:0}")
    private long processingDelayMs;

    public AiAnalysisConsumer(
            MessageProcessingResultService resultService
    ) {
        this.resultService = resultService;
    }

    @RabbitListener(
            queues = RabbitMqConfig.DOCUMENT_PROCESSED_QUEUE,
            concurrency = "${DOCFLOW_CONSUMER_CONCURRENCY:1}"
    )
    public void analyze(DocumentProcessedEvent event) {

        long startedAt = System.nanoTime();

        log.info(
                "CONSUMER 2 RECEIVED: eventId={}, sourceEventId={}, documentId={}, documentType={}",
                event.eventId(),
                event.sourceEventId(),
                event.documentId(),
                event.documentType()
        );

        validate(event);

        simulateProcessing();

        String analysisResult =
                performAnalysis(event);

        long processingTimeMs =
                (System.nanoTime() - startedAt) / 1_000_000;

        resultService.save(
                event.eventId(),
                event.sourceEventId(),
                event.eventType(),
                event.documentId(),
                "AI Analysis Service",
                analysisResult,
                processingTimeMs
        );

        log.info(
                "CONSUMER 2 PROCESSED: eventId={}, documentId={}, result={}, processingTimeMs={}",
                event.eventId(),
                event.documentId(),
                analysisResult,
                processingTimeMs
        );
    }

    private void validate(DocumentProcessedEvent event) {

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

        if (event.processingStatus() == null ||
                event.processingStatus().isBlank()) {
            throw new IllegalArgumentException(
                    "processingStatus is required"
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

    private String performAnalysis(
            DocumentProcessedEvent event
    ) {
        return "AI_ANALYSIS_COMPLETED";
    }
}