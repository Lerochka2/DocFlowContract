package by.bsuir.docflow.docflow_backend.messaging.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageProcessingResultRepository
        extends JpaRepository<MessageProcessingResult, Long> {
}