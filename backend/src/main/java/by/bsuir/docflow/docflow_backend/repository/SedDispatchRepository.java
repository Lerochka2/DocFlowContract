package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.SedDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SedDispatchRepository
        extends JpaRepository<SedDispatch, Long> {

    List<SedDispatch>
    findByDocumentIdOrderByCreatedAtDesc(
            Long documentId
    );

    Optional<SedDispatch>
    findFirstByDocumentIdOrderByCreatedAtDesc(
            Long documentId
    );
}