package by.bsuir.docflow.docflow_backend.dto;

public class NotificationUnreadCountResponse {

    private long unreadCount;

    public NotificationUnreadCountResponse(
            long unreadCount
    ) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(
            long unreadCount
    ) {
        this.unreadCount = unreadCount;
    }
}
