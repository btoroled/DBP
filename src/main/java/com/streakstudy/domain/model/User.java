package com.streakstudy.domain.model;

import com.streakstudy.domain.exception.DomainException;
import com.streakstudy.domain.exception.InsufficientXpException;
import com.streakstudy.domain.exception.MaxStreakFreezesReachedException;

import java.time.Instant;
import java.util.Objects;
import java.time.LocalDate;

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
                int streakFreezes) {
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
    }

    public static User newStudent(Long institutionId, String email, String passwordHash, String fullName) {
        return new User(null, institutionId, email, passwordHash, fullName, UserRole.STUDENT, null,0,0,null,0);
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
                newStreak, today, 0
        );
    }

    public User resetStreak() {
        if (this.currentStreak == 0) return this;
        return new User(
                this.id(), this.institutionId(), this.email(), this.passwordHash(),
                this.fullName(), this.role(), this.createdAt(), this.xp(),
                0, this.lastActiveDate(),0
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
                this.streakFreezes()
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
                this.streakFreezes + 1
        );
    }

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
