package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.auth.ConfirmPasswordChangeRequest;
import by.bsuir.docflow.docflow_backend.dto.auth.PasswordCodeRequestResponse;
import by.bsuir.docflow.docflow_backend.dto.auth.PasswordCodeVerificationResponse;
import by.bsuir.docflow.docflow_backend.dto.auth.VerifyPasswordCodeRequest;
import by.bsuir.docflow.docflow_backend.service.PasswordChangeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/auth/password-change"
)
public class PasswordChangeController {

    private final PasswordChangeService
            passwordChangeService;


    public PasswordChangeController(
            PasswordChangeService passwordChangeService
    ) {
        this.passwordChangeService =
                passwordChangeService;
    }


    @PostMapping("/request-code")
    public PasswordCodeRequestResponse requestCode(
            @AuthenticationPrincipal
            Jwt jwt
    ) {

        return passwordChangeService
                .requestCode(
                        getUserId(jwt)
                );
    }


    @PostMapping("/verify-code")
    public PasswordCodeVerificationResponse verifyCode(
            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            VerifyPasswordCodeRequest request
    ) {

        return passwordChangeService
                .verifyCode(
                        getUserId(jwt),
                        request.getCode()
                );
    }


    @PutMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            ConfirmPasswordChangeRequest request
    ) {

        passwordChangeService
                .confirmPasswordChange(
                        getUserId(jwt),
                        request
                );

        return ResponseEntity
                .noContent()
                .build();
    }


    private Long getUserId(
            Jwt jwt
    ) {

        Number userId =
                jwt.getClaim(
                        "userId"
                );

        if (userId == null) {

            throw new IllegalStateException(
                    "JWT не содержит userId"
            );
        }

        return userId.longValue();
    }
}