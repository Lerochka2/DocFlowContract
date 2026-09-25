package by.bsuir.docflow.docflow_backend.messaging.persistence;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageProcessingResultService {

    private final MessageProcessingResultRepository repository;

    public MessageProcessingResultService(
            MessageProcessingResultRepository repository
    ) {
        this.repository = repository;
    }

    public void save(
            String eventId,
            String sourceEventId,
            String eventType,
            Long documentId,
            String consumerName,
            String status,
            long processingTimeMs
    ) {

        MessageProcessingResult result =
                new MessageProcessingResult(
                        eventId,
                        sourceEventId,
                        eventType,
                        documentId,
                        consumerName,
                        status,
                        processingTimeMs,
                        LocalDateTime.now()
                );

        repository.save(result);
    }
}