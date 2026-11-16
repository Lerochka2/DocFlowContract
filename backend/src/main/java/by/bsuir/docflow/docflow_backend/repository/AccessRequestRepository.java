package by.bsuir.docflow.docflow_backend.repository;
import by.bsuir.docflow.docflow_backend.entity.AccessRequest;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface AccessRequestRepository extends JpaRepository<AccessRequest,Long> {
    List<AccessRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
    boolean existsByEmailAndStatus(String email,String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccessRequest a where a.id = :id")
    Optional<AccessRequest> findLockedById(Long id);
}
