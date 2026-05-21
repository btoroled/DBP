package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.User;
import com.streakstudy.infrastructure.persistence.entity.UserJpa;

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
                jpa.getStreak(),  // <-- NUEVO: Pasa la racha al dominio
                jpa.getPoints()   // <-- NUEVO: Pasa los puntos al dominio
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
        jpa.setStreak(domain.streak());   // <-- NUEVO: Guarda la racha en la BD
        jpa.setPoints(domain.points());   // <-- NUEVO: Guarda los puntos en la BD
        return jpa;
    }
}