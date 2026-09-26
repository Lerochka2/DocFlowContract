package by.bsuir.docflow.docflow_backend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReminderRequest(

        @NotBlank(
                message = "Введите текст напоминания"
        )
        @Size(
                max = 1000,
                message = "Напоминание не должно превышать 1000 символов"
        )
        String text

) {
}