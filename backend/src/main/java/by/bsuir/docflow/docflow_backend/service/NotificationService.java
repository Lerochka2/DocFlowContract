package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.NotificationResponse;
import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.UserNotification;
import by.bsuir.docflow.docflow_backend.entity.enums.NotificationType;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.UserNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    public NotificationService(
            UserNotificationRepository notificationRepository,
            AppUserRepository appUserRepository
    ) {
        this.notificationRepository =
                notificationRepository;

        this.appUserRepository =
                appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getCurrentUserNotifications() {

        AppUser currentUser =
                resolveCurrentUser();

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        currentUser.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {

        AppUser currentUser =
                resolveCurrentUser();

        return notificationRepository
                .countByUserIdAndReadFalse(
                        currentUser.getId()
                );
    }

    @Transactional
    public NotificationResponse markAsRead(
            Long notificationId
    ) {

        AppUser currentUser =
                resolveCurrentUser();

        UserNotification notification =
                notificationRepository
                        .findByIdAndUserId(
                                notificationId,
                                currentUser.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Уведомление не найдено"
                                        )
                        );

        if (!Boolean.TRUE.equals(
                notification.getRead()
        )) {
            notification.setRead(true);
            notification.setReadAt(
                    LocalDateTime.now()
            );
        }

        return toResponse(
                notificationRepository.save(
                        notification
                )
        );
    }

    @Transactional
    public void markAllAsRead() {

        AppUser currentUser =
                resolveCurrentUser();

        List<UserNotification> unreadNotifications =
                notificationRepository
                        .findByUserIdAndReadFalse(
                                currentUser.getId()
                        );

        LocalDateTime now =
                LocalDateTime.now();

        for (
                UserNotification notification :
                unreadNotifications
        ) {
            notification.setRead(true);
            notification.setReadAt(now);
        }

        notificationRepository.saveAll(
                unreadNotifications
        );
    }

    @Transactional
    public UserNotification createNotification(
            AppUser user,
            Document document,
            NotificationType type,
            String title,
            String message,
            String route,
            String actionLabel,
            String dedupKey
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Пользователь уведомления не указан"
            );
        }

        if (
                dedupKey != null
                && !dedupKey.isBlank()
                && notificationRepository
                .existsByUserIdAndDedupKey(
                        user.getId(),
                        dedupKey
                )
        ) {
            return null;
        }

        UserNotification notification =
                new UserNotification();

        notification.setUser(user);
        notification.setDocument(document);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRoute(route);
        notification.setActionLabel(actionLabel);

        if (
                dedupKey != null
                && !dedupKey.isBlank()
        ) {
            notification.setDedupKey(
                    dedupKey.trim()
            );
        }

        notification.setRead(false);

        return notificationRepository.save(
                notification
        );
    }

    private NotificationResponse toResponse(
            UserNotification notification
    ) {

        NotificationResponse response =
                new NotificationResponse();

        response.setId(
                notification.getId()
        );

        if (
                notification.getDocument()
                != null
        ) {
            response.setDocumentId(
                    notification
                            .getDocument()
                            .getId()
            );
        }

        response.setType(
                notification
                        .getNotificationType()
                        .name()
        );

        response.setTitle(
                notification.getTitle()
        );

        response.setMessage(
                notification.getMessage()
        );

        response.setRoute(
                notification.getRoute()
        );

        response.setActionLabel(
                notification.getActionLabel()
        );

        response.setRead(
                notification.getRead()
        );

        response.setCreatedAt(
                notification.getCreatedAt()
        );

        return response;
    }

    private AppUser resolveCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                || !authentication.isAuthenticated()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Пользователь не авторизован"
            );
        }

        return appUserRepository
                .findByLogin(
                        authentication.getName()
                )
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Текущий пользователь не найден"
                                )
                );
    }
}
