package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentDecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentDecisionRepository
        extends JpaRepository<DocumentDecisionRecord, Long> {

    List<DocumentDecisionRecord>
    findByDocument_IdOrderByCreatedAtAsc(
            Long documentId
    );
}