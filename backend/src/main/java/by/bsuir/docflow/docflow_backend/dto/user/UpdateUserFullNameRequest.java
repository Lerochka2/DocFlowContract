package by.bsuir.docflow.docflow_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserFullNameRequest(

        @NotBlank(message = "Необходимо указать ФИО")
        @Size(
                max = 255,
                message = "ФИО не должно превышать 255 символов"
        )
        String fullName

) {
}