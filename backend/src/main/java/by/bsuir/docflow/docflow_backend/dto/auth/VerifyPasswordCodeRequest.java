package by.bsuir.docflow.docflow_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyPasswordCodeRequest {

    @NotBlank(
            message = "Введите код подтверждения"
    )
    @Pattern(
            regexp = "\\d{6}",
            message = "Код должен состоять из 6 цифр"
    )
    private String code;


    public VerifyPasswordCodeRequest() {
    }


    public String getCode() {
        return code;
    }


    public void setCode(
            String code
    ) {
        this.code = code;
    }
}