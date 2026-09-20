package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository
        extends JpaRepository<UserNotification, Long> {

    List<UserNotification>
    findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserNotification>
    findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    List<UserNotification>
    findByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndDedupKey(
            Long userId,
            String dedupKey
    );
}
