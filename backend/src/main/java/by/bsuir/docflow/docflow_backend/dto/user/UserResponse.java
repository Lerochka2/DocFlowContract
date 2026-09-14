package by.bsuir.docflow.docflow_backend.dto.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String login,
        String fullName,
        String roleCode,
        String roleName,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {
}