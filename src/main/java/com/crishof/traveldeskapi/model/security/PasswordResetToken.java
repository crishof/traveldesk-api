package com.crishof.traveldeskapi.model.security;

import com.crishof.traveldeskapi.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tbl_password_reset_tokens",
        indexes = {
        @Index(name = "idx_reset_token_token", columnList = "token"),
                @Index(name = "idx_reset_token_expiry", columnList = "expiry_date")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // ============================
    // BUSINESS HELPERS
    // ============================

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}