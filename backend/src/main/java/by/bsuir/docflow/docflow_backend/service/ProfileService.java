package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.auth.CurrentUserResponse;
import by.bsuir.docflow.docflow_backend.dto.profile.CreateReminderRequest;
import by.bsuir.docflow.docflow_backend.dto.profile.UserReminderResponse;
import by.bsuir.docflow.docflow_backend.dto.user.UpdateUserFullNameRequest;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.UserReminder;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.UserReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProfileService {

    private final AppUserRepository userRepository;

    private final UserReminderRepository reminderRepository;


    public ProfileService(
            AppUserRepository userRepository,
            UserReminderRepository reminderRepository
    ) {
        this.userRepository =
                userRepository;

        this.reminderRepository =
                reminderRepository;
    }


    @Transactional
    public CurrentUserResponse updateFullName(
            Long userId,
            UpdateUserFullNameRequest request
    ) {

        AppUser user =
                getActiveUser(
                        userId
                );

        String fullName =
                request
                        .fullName()
                        .trim();

        user.setFullName(
                fullName
        );

        userRepository.save(
                user
        );

        return toCurrentUserResponse(
                user
        );
    }


    @Transactional(readOnly = true)
    public List<UserReminderResponse> getReminders(
            Long userId
    ) {

        getActiveUser(
                userId
        );

        return reminderRepository
                .findAllByUser_IdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(
                        this::toReminderResponse
                )
                .toList();
    }


    @Transactional
    public UserReminderResponse createReminder(
            Long userId,
            CreateReminderRequest request
    ) {

        AppUser user =
                getActiveUser(
                        userId
                );

        UserReminder reminder =
                new UserReminder();

        reminder.setUser(
                user
        );

        reminder.setText(
                request
                        .text()
                        .trim()
        );

        return toReminderResponse(
                reminderRepository.save(
                        reminder
                )
        );
    }


    @Transactional
    public void deleteReminder(
            Long userId,
            Long reminderId
    ) {

        UserReminder reminder =
                reminderRepository
                        .findById(
                                reminderId
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Напоминание не найдено"
                                        )
                        );

        if (
                !reminder
                        .getUser()
                        .getId()
                        .equals(
                                userId
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Напоминание не найдено"
            );
        }

        reminderRepository.delete(
                reminder
        );
    }


    private AppUser getActiveUser(
            Long userId
    ) {

        AppUser user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Пользователь не найден"
                                        )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getEnabled()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Учетная запись отключена"
            );
        }

        return user;
    }


    private CurrentUserResponse toCurrentUserResponse(
            AppUser user
    ) {

        return new CurrentUserResponse(
                user.getId(),
                user.getLogin(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getCode()
        );
    }


    private UserReminderResponse toReminderResponse(
            UserReminder reminder
    ) {

        return new UserReminderResponse(
                reminder.getId(),
                reminder.getText(),
                reminder.getCreatedAt()
        );
    }
}