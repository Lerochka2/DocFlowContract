package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    boolean existsBySha256Checksum(String sha256Checksum);
}