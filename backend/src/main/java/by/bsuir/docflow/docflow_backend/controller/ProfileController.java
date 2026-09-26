package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.auth.CurrentUserResponse;
import by.bsuir.docflow.docflow_backend.dto.profile.CreateReminderRequest;
import by.bsuir.docflow.docflow_backend.dto.profile.UserReminderResponse;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserFullNameRequest;
import by.bsuir.docflow.docflow_backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;


    public ProfileController(
            ProfileService profileService
    ) {
        this.profileService =
                profileService;
    }


    @PatchMapping("/full-name")
    public CurrentUserResponse updateFullName(
            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            UpdateUserFullNameRequest request
    ) {

        return profileService
                .updateFullName(
                        getUserId(jwt),
                        request
                );
    }


    @GetMapping("/reminders")
    public List<UserReminderResponse> getReminders(
            @AuthenticationPrincipal
            Jwt jwt
    ) {

        return profileService
                .getReminders(
                        getUserId(jwt)
                );
    }


    @PostMapping("/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    public UserReminderResponse createReminder(
            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            CreateReminderRequest request
    ) {

        return profileService
                .createReminder(
                        getUserId(jwt),
                        request
                );
    }


    @DeleteMapping("/reminders/{reminderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReminder(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            Long reminderId
    ) {

        profileService
                .deleteReminder(
                        getUserId(jwt),
                        reminderId
                );
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
                    "JWT не содержит идентификатор пользователя"
            );
        }

        return userId.longValue();
    }
}