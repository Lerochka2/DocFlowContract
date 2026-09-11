package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}