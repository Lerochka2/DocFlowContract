package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.UserReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReminderRepository
        extends JpaRepository<UserReminder, Long> {

    List<UserReminder>
    findAllByUser_IdOrderByCreatedAtDesc(
            Long userId
    );
}