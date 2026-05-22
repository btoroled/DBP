package com.streakstudy.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.UserRole;

import jakarta.persistence.*;

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

    @Column(nullable = false)
    private int xp = 0;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "streak_freezes", nullable = false)
    private int streakFreezes = 0;

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

    @ElementCollection(targetClass = Badge.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "user_badges", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "badge_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Badge> badges = new HashSet<>();


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

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public LocalDate getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(LocalDate lastActiveDate) { this.lastActiveDate = lastActiveDate; }

    public int getStreakFreezes() { return streakFreezes; }
    public void setStreakFreezes(int streakFreezes) { this.streakFreezes = streakFreezes; }

    public Set<Badge> getBadges() { return badges; }
    public void setBadges(Set<Badge> badges) { this.badges = badges; }
}
