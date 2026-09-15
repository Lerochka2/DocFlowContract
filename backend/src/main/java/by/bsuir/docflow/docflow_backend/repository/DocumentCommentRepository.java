package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentCommentRepository
        extends JpaRepository<DocumentComment, Long> {

    List<DocumentComment>
    findByDocument_IdOrderByCreatedAtAsc(
            Long documentId
    );
}