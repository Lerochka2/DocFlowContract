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
@Table(name = "password_verification_codes")
public class PasswordVerificationCode {

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
            name = "code_hash",
            nullable = false,
            length = 255
    )
    private String codeHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private Integer attemptCount = 0;

    @Column(
            nullable = false
    )
    private Boolean used = false;

    @Column(
            name = "verified_at"
    )
    private LocalDateTime verifiedAt;

    @Column(
            name = "change_token_hash",
            length = 255
    )
    private String changeTokenHash;

    @Column(
            name = "change_token_expires_at"
    )
    private LocalDateTime changeTokenExpiresAt;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    public PasswordVerificationCode() {
    }


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt =
                    LocalDateTime.now();
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }

        if (used == null) {
            used = false;
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
        this.user = user;
    }


    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(
            String codeHash
    ) {
        this.codeHash = codeHash;
    }


    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(
            LocalDateTime expiresAt
    ) {
        this.expiresAt = expiresAt;
    }


    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(
            Integer attemptCount
    ) {
        this.attemptCount = attemptCount;
    }


    public Boolean getUsed() {
        return used;
    }

    public void setUsed(
            Boolean used
    ) {
        this.used = used;
    }


    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(
            LocalDateTime verifiedAt
    ) {
        this.verifiedAt = verifiedAt;
    }


    public String getChangeTokenHash() {
        return changeTokenHash;
    }

    public void setChangeTokenHash(
            String changeTokenHash
    ) {
        this.changeTokenHash =
                changeTokenHash;
    }


    public LocalDateTime getChangeTokenExpiresAt() {
        return changeTokenExpiresAt;
    }

    public void setChangeTokenExpiresAt(
            LocalDateTime changeTokenExpiresAt
    ) {
        this.changeTokenExpiresAt =
                changeTokenExpiresAt;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}