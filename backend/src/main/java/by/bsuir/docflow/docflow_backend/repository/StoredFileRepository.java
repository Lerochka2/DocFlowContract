package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository
        extends JpaRepository<StoredFile, Long> {

    boolean existsBySha256Checksum(
            String sha256Checksum
    );

    Optional<StoredFile>
    findFirstBySha256Checksum(
            String sha256Checksum
    );
}