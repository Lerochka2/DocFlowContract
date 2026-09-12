package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.AiProcessingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProcessingResultRepository
        extends JpaRepository<AiProcessingResult, Long> {

    List<AiProcessingResult>
    findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    Optional<AiProcessingResult>
    findFirstByDocumentIdOrderByCreatedAtDesc(Long documentId);
}