package by.bsuir.docflow.docflow_backend.dto.profile;

import java.time.LocalDateTime;

public record UserReminderResponse(
        Long id,
        String text,
        LocalDateTime createdAt
) {
}