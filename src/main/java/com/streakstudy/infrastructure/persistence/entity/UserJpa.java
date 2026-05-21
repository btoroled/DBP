package com.streakstudy.infrastructure.persistence.entity;

import java.time.Instant;

import com.streakstudy.domain.model.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidad JPA para User. Tenant-aware (extiende {@link TenantAwareJpaEntity}).
 *
 * <p>El email es <b>globalmente unico</b> (no por tenant) porque el login es
 * cross-tenant: el usuario solo proporciona email y password, y nosotros
 * resolvemos su tenant a partir del registro.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        indexes = {
                @Index(name = "ix_users_institution", columnList = "institution_id"),
                @Index(name = "ix_users_email", columnList = "email")
        }
)
public class UserJpa extends TenantAwareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ========================================================
    // NUEVOS CAMPOS AGREGADOS PARA TU PARTE (UI #11, #12, #15)
    // ========================================================
    @Column(name = "streak", nullable = false)
    private Integer streak = 0;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (streak == null) streak = 0;
        if (points == null) points = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // GETTERS Y SETTERS DE LOS NUEVOS CAMPOS
    public Integer getStreak() { return streak; }
    public void setStreak(Integer streak) { this.streak = streak; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}
