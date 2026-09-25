package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.user.CreateUserRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserEmailRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserEnabledRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserFullNameRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserRoleRequest;
import by.bsuir.docflow.docflow_backend.dto.user.UserResponse;
import by.bsuir.docflow.docflow_backend.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(
            UserAdminService userAdminService
    ) {
        this.userAdminService =
                userAdminService;
    }

    @GetMapping
    public List<UserResponse> getUsers() {

        return userAdminService
                .getUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        return userAdminService
                .createUser(
                        request
                );
    }

    @PatchMapping("/{userId}/full-name")
    public UserResponse updateFullName(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserFullNameRequest request
    ) {

        return userAdminService
                .updateFullName(
                        userId,
                        request
                );
    }

    @PatchMapping("/{userId}/email")
    public UserResponse updateEmail(
            @PathVariable Long userId,
            @Valid
            @RequestBody
            UpdateUserEmailRequest request
    ) {

        return userAdminService
                .updateEmail(
                        userId,
                        request
                );
    }


    @PatchMapping("/{userId}/role")
    public UserResponse updateRole(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {

        return userAdminService
                .updateRole(
                        userId,
                        getCurrentUserId(jwt),
                        request
                );
    }

    @PatchMapping("/{userId}/enabled")
    public UserResponse updateEnabled(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserEnabledRequest request
    ) {

        return userAdminService
                .updateEnabled(
                        userId,
                        getCurrentUserId(jwt),
                        request
                );
    }

    private Long getCurrentUserId(
            Jwt jwt
    ) {

        Number userId =
                jwt.getClaim(
                        "userId"
                );

        if (userId == null) {

            throw new IllegalStateException(
                    "JWT не содержит идентификатор пользователя"
            );
        }

        return userId.longValue();
    }
}