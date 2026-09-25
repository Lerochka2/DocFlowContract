package by.bsuir.docflow.docflow_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfirmPasswordChangeRequest {

    @NotBlank(
            message = "Отсутствует токен подтверждения"
    )
    private String changeToken;

    @NotBlank(
            message = "Введите новый пароль"
    )
    @Size(
            min = 8,
            max = 100,
            message = "Пароль должен содержать от 8 до 100 символов"
    )
    private String newPassword;

    @NotBlank(
            message = "Повторите новый пароль"
    )
    private String confirmPassword;


    public ConfirmPasswordChangeRequest() {
    }


    public String getChangeToken() {
        return changeToken;
    }

    public void setChangeToken(
            String changeToken
    ) {
        this.changeToken =
                changeToken;
    }


    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(
            String newPassword
    ) {
        this.newPassword =
                newPassword;
    }


    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(
            String confirmPassword
    ) {
        this.confirmPassword =
                confirmPassword;
    }
}