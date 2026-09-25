package by.bsuir.docflow.docflow_backend.messaging.lab;

import by.bsuir.docflow.docflow_backend.messaging.event.DocumentUploadedEvent;
import by.bsuir.docflow.docflow_backend.messaging.producer.DocumentEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "docflow.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LabProducerRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(LabProducerRunner.class);

    private final DocumentEventProducer producer;

    @Value("${DOCFLOW_LAB_TEST_PRODUCER:false}")
    private boolean enabled;

    @Value("${DOCFLOW_LAB_MESSAGE_COUNT:1}")
    private int messageCount;

    public LabProducerRunner(DocumentEventProducer producer) {
        this.producer = producer;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (!enabled) {
            return;
        }

        long startedAt = System.nanoTime();

        for (int i = 1; i <= messageCount; i++) {

            DocumentUploadedEvent event =
                    DocumentUploadedEvent.create(
                            1000L + i,
                            "contract_" + i + ".pdf",
                            "storage/contracts/contract_" + i + ".pdf",
                            "UPLOADED"
                    );

            producer.sendDocumentUploaded(event);
        }

        long elapsedMs =
                (System.nanoTime() - startedAt) / 1_000_000;

        log.info(
                "LAB PRODUCER TEST COMPLETED: sent={}, elapsedMs={}",
                messageCount,
                elapsedMs
        );
    }
}