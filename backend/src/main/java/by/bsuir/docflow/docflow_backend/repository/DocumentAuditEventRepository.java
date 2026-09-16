package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAuditEventRepository
        extends JpaRepository<DocumentAuditEvent, Long> {

    List<DocumentAuditEvent>
    findByDocument_IdOrderByCreatedAtAsc(
            Long documentId
    );
}