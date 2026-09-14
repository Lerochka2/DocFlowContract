package by.bsuir.docflow.docflow_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Необходимо указать логин")
        @Size(
                min = 3,
                max = 100,
                message = "Логин должен содержать от 3 до 100 символов"
        )
        String login,

        @NotBlank(message = "Необходимо указать ФИО")
        @Size(
                max = 255,
                message = "ФИО не должно превышать 255 символов"
        )
        String fullName,

        @NotBlank(message = "Необходимо указать пароль")
        @Size(
                min = 8,
                max = 100,
                message = "Пароль должен содержать не менее 8 символов"
        )
        String password,

        @NotBlank(message = "Необходимо указать роль")
        String roleCode
) {
}