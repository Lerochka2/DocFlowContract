package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRouteRepository
        extends JpaRepository<DocumentRoute, Long> {

    List<DocumentRoute>
    findByDocumentIdOrderByCreatedAtDesc(
            Long documentId
    );

    Optional<DocumentRoute>
    findFirstByDocumentIdOrderByCreatedAtDesc(
            Long documentId
    );
}