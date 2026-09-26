package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository
        extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion>
    findByDocumentIdOrderByVersionNumberDesc(
            Long documentId
    );

    Optional<DocumentVersion>
    findFirstByDocumentIdOrderByVersionNumberDesc(
            Long documentId
    );

    Optional<DocumentVersion>
    findByDocumentIdAndVersionNumber(
            Long documentId,
            Integer versionNumber
    );

    Optional<DocumentVersion>
    findByDocumentIdAndFileId(
            Long documentId,
            Long fileId
    );
}