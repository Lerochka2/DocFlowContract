package by.bsuir.docflow.docflow_backend.messaging.producer;

import by.bsuir.docflow.docflow_backend.config.RabbitMqConfig;
import by.bsuir.docflow.docflow_backend.messaging.event.DocumentProcessedEvent;
import by.bsuir.docflow.docflow_backend.messaging.event.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "docflow.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DocumentEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDocumentUploaded(DocumentUploadedEvent event) {

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.DOCUMENT_UPLOADED_ROUTING_KEY,
                event
        );

        log.info(
                "RabbitMQ SENT: eventId={}, eventType={}, documentId={}, exchange={}, routingKey={}",
                event.eventId(),
                event.eventType(),
                event.documentId(),
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.DOCUMENT_UPLOADED_ROUTING_KEY
        );
    }

    public void sendDocumentProcessed(DocumentProcessedEvent event) {

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.DOCUMENT_PROCESSED_ROUTING_KEY,
                event
        );

        log.info(
                "RabbitMQ SENT: eventId={}, eventType={}, sourceEventId={}, documentId={}, exchange={}, routingKey={}",
                event.eventId(),
                event.eventType(),
                event.sourceEventId(),
                event.documentId(),
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.DOCUMENT_PROCESSED_ROUTING_KEY
        );
    }
}