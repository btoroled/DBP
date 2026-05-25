package com.streakstudy.domain.model;

import com.streakstudy.domain.exception.BadgeAlreadyOwnedException;
import com.streakstudy.domain.exception.InsufficientXpException;
import com.streakstudy.domain.exception.MaxStreakFreezesReachedException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.time.LocalDate;
import java.util.Set;

/**
 * Usuario de la plataforma. Tenant-aware: cada usuario pertenece a una institucion.
 *
 * <p>El campo {@code passwordHash} es el hash BCrypt (no la contrasena plana).</p>
 */
public final class User implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UserRole role;
    private final Instant createdAt;
    private final int xp;
    private final int currentStreak;
    private final LocalDate lastActiveDate;
    private final int streakFreezes;
    private final Set<Badge> badges;

    public User(Long id,
                Long institutionId,
                String email,
                String passwordHash,
                String fullName,
                UserRole role,
                Instant createdAt,
                int xp,
                int currentStreak,
                LocalDate lastActiveDate,
                int streakFreezes,
                Set<Badge> badges) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.email = Objects.requireNonNull(email, "email").toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = createdAt;
        this.xp = xp;
        this.currentStreak = currentStreak;
        this.lastActiveDate = lastActiveDate;
        this.streakFreezes = streakFreezes;
        this.badges = badges != null ? Set.copyOf(badges): Set.of();
    }

    public static User newStudent(Long institutionId, String email, String passwordHash, String fullName) {
        return new User(null, institutionId, email, passwordHash, fullName, UserRole.STUDENT, null,0,0,null,0,Set.of());
    }

    public Long id() { return id; }
    @Override public Long institutionId() { return institutionId; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String fullName() { return fullName; }
    public UserRole role() { return role; }
    public Instant createdAt() { return createdAt; }
    public int xp(){return xp;}
    public  int currentStreak(){return currentStreak;}
    public LocalDate lastActiveDate() { return lastActiveDate; }
    public int streakFreezes(){return streakFreezes;}
    public Set<Badge> badges(){return badges;}


    public User incrementStreak(LocalDate today) {
        if (lastActiveDate != null && lastActiveDate.equals(today)) {
            return this;
        }

        int newStreak = 1;
        if (lastActiveDate != null && lastActiveDate.equals(today.minusDays(1))) {
            newStreak = this.currentStreak + 1;
        }

        return new User(
                this.id(), this.institutionId(), this.email(), this.passwordHash(),
                this.fullName(), this.role(), this.createdAt(), this.xp(),
                newStreak, today, 0,Set.of()
        );
    }


    public User addXp(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("No se puede sumar XP negativo");
        }
        return new User(
                this.id,
                this.institutionId,
                this.email,
                this.passwordHash,
                this.fullName,
                this.role,
                this.createdAt,
                this.xp + amount,
                this.currentStreak,
                this.lastActiveDate,
                this.streakFreezes(),
                this.badges()
        );
    }

    public User buyStreakFreeze() {
        if (this.streakFreezes >= 2) {
            throw new MaxStreakFreezesReachedException();
        }
        if (this.xp < 5) {
            throw new InsufficientXpException();
        }

        return new User(
                this.id(),
                this.institutionId(),
                this.email(),
                this.passwordHash(),
                this.fullName(),
                this.role(),
                this.createdAt(),
                this.xp() - 5,
                this.currentStreak(),
                this.lastActiveDate(),
                this.streakFreezes + 1,
                this.badges()
        );
    }

    public User buyBadge(Badge badge) {
        if (this.badges.contains(badge)) {
            throw new BadgeAlreadyOwnedException(badge.name());
        }
        if (this.xp < 7) {
            throw new InsufficientXpException();
        }

        Set<Badge> newBadges = new HashSet<>(this.badges);
        newBadges.add(badge);

        return new User(
                this.id(), this.institutionId(), this.email(), this.passwordHash(),
                this.fullName(), this.role(), this.createdAt(), this.xp() - 7,
                this.currentStreak(), this.lastActiveDate(), this.streakFreezes(), newBadges
        );
    }

    public User useStreakFreeze() {
        return new User(
                this.id,
                this.institutionId,
                this.email,
                this.passwordHash,
                this.fullName,
                this.role,
                this.createdAt,
                this.xp,
                this.currentStreak,
                java.time.LocalDate.now(),
                this.streakFreezes - 1,
                this.badges
        );
    }

    public User resetStreakToZero() {
        return new User(
                this.id,
                this.institutionId,
                this.email,
                this.passwordHash,
                this.fullName,
                this.role,
                this.createdAt,
                this.xp,
                0,
                this.lastActiveDate,
                this.streakFreezes,
                this.badges
        );
    }

    public User withPasswordHash(String newPasswordHash) {
        return new User(
                this.id,
                this.institutionId,
                this.email,
                Objects.requireNonNull(newPasswordHash, "passwordHash"),
                this.fullName,
                this.role,
                this.createdAt,
                this.xp,
                this.currentStreak,
                this.lastActiveDate,
                this.streakFreezes,
                this.badges
        );
    }

    public Integer streak() { return currentStreak; }
    public Integer points() { return xp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }


}
