package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentLinkRepository
        extends JpaRepository<DocumentLink, Long> {

    Optional<DocumentLink>
    findByChildDocumentId(
            Long childDocumentId
    );

    boolean existsByChildDocumentId(
            Long childDocumentId
    );

    List<DocumentLink>
    findByContractDocumentIdOrderByCreatedAtDesc(
            Long contractDocumentId
    );
}