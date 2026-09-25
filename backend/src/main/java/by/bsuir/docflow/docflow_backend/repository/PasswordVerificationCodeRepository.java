package by.bsuir.docflow.docflow_backend.repository;

import by.bsuir.docflow.docflow_backend.entity.PasswordVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordVerificationCodeRepository
        extends JpaRepository<
                PasswordVerificationCode,
                Long
        > {

    Optional<PasswordVerificationCode>
    findTopByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<PasswordVerificationCode>
    findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(
            Long userId
    );

    List<PasswordVerificationCode>
    findByUser_IdAndUsedFalse(
            Long userId
    );
}