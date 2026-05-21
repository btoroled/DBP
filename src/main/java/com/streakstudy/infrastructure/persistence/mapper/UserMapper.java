package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.User;
import com.streakstudy.infrastructure.persistence.entity.UserJpa;

import java.util.HashSet;
import java.util.Set;

public final class UserMapper {

    private UserMapper() { }

    public static User toDomain(UserJpa jpa) {
        return new User(
            jpa.getId(),
            jpa.getInstitutionId(),
            jpa.getEmail(),
            jpa.getPasswordHash(),
            jpa.getFullName(),
            jpa.getRole(),
            jpa.getCreatedAt(),
                jpa.getXp(),
                jpa.getCurrentStreak(),
                jpa.getLastActiveDate(),
                jpa.getStreakFreezes(),
                jpa.getBadges() != null ? Set.copyOf(jpa.getBadges()) : Set.of()
        );
    }

    public static UserJpa toJpa(User domain) {
        UserJpa jpa = new UserJpa();
        jpa.setId(domain.id());
        jpa.setInstitutionId(domain.institutionId());
        jpa.setEmail(domain.email());
        jpa.setPasswordHash(domain.passwordHash());
        jpa.setFullName(domain.fullName());
        jpa.setRole(domain.role());
        jpa.setCreatedAt(domain.createdAt());
        jpa.setXp(domain.xp());
        jpa.setCurrentStreak(domain.currentStreak());
        jpa.setLastActiveDate(domain.lastActiveDate());
        jpa.setStreakFreezes(domain.streakFreezes());
        jpa.setBadges(domain.badges() != null ? new HashSet<>(domain.badges()) : new HashSet<>());
        return jpa;
    }
}
