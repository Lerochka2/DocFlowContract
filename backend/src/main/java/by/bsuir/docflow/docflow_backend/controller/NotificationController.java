package by.bsuir.docflow.docflow_backend.controller;

import by.bsuir.docflow.docflow_backend.dto.NotificationResponse;
import by.bsuir.docflow.docflow_backend.dto.NotificationUnreadCountResponse;
import by.bsuir.docflow.docflow_backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService =
                notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications() {

        return notificationService
                .getCurrentUserNotifications();
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse getUnreadCount() {

        return new NotificationUnreadCountResponse(
                notificationService.getUnreadCount()
        );
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId
    ) {

        return notificationService
                .markAsRead(notificationId);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {

        notificationService.markAllAsRead();

        return ResponseEntity.noContent().build();
    }
}
