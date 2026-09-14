package by.bsuir.docflow.docflow_backend.dto.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserEnabledRequest(

        @NotNull(message = "Необходимо указать состояние пользователя")
        Boolean enabled

) {
}