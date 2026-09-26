package by.bsuir.docflow.docflow_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_reminders")
public class UserReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private AppUser user;

    @Column(
            name = "reminder_text",
            nullable = false,
            length = 1000
    )
    private String text;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    public UserReminder() {
    }


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt =
                    LocalDateTime.now();
        }
    }


    public Long getId() {
        return id;
    }


    public AppUser getUser() {
        return user;
    }


    public void setUser(
            AppUser user
    ) {
        this.user =
                user;
    }


    public String getText() {
        return text;
    }


    public void setText(
            String text
    ) {
        this.text =
                text;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}