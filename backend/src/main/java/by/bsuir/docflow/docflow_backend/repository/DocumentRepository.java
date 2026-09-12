package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();
}