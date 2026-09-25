package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.auth.CurrentUserResponse;
import by.bsuir.docflow.docflow_backend.dto.auth.LoginRequest;
import by.bsuir.docflow.docflow_backend.dto.auth.LoginResponse;
import by.bsuir.docflow.docflow_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;


    public AuthController(
            AuthService authService
    ) {
        this.authService =
                authService;
    }


    @PostMapping("/login")
    public LoginResponse login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {

        return authService.login(
                request
        );
    }


    @GetMapping("/me")
    public CurrentUserResponse me(
            @AuthenticationPrincipal
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

        return authService
                .getCurrentUser(
                        userId.longValue()
                );
    }
}