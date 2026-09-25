package by.bsuir.docflow.docflow_backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserEmailRequest(

        @NotBlank(
                message = "Необходимо указать email"
        )
        @Email(
                message = "Некорректный формат email"
        )
        @Size(
                max = 255,
                message = "Email не должен превышать 255 символов"
        )
        String email

) {
}