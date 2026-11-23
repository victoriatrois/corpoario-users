package br.com.corpoario.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "user_accounts")
public class Account {
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[abxy]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Account() {
    }

    public Account(String email, String passwordHash, AccountRole role) {
        this.email = normaliseEmail(email);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.role = java.util.Objects.requireNonNull(role, "role is required");
        this.status = AccountStatus.ACTIVE;
    }

    public static String normaliseEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public AccountRole getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || !BCRYPT_HASH.matcher(passwordHash).matches()) {
            throw new IllegalArgumentException("passwordHash must be a BCrypt hash");
        }
        return passwordHash;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        email = normaliseEmail(email);
        passwordHash = requirePasswordHash(passwordHash);
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        email = normaliseEmail(email);
        passwordHash = requirePasswordHash(passwordHash);
        updatedAt = LocalDateTime.now();
    }
}
