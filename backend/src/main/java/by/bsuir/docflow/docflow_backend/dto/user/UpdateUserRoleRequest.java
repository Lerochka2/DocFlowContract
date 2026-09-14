package by.bsuir.docflow.docflow_backend.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(

        @NotBlank(message = "Необходимо указать роль")
        String roleCode

) {
}