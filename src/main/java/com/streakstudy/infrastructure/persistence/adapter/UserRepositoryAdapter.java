package com.streakstudy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.persistence.entity.UserJpa;
import com.streakstudy.infrastructure.persistence.mapper.UserMapper;
import com.streakstudy.infrastructure.persistence.repository.UserJpaRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return TenantContext.runCrossTenant(() -> {
            UserJpa saved = jpa.save(UserMapper.toJpa(user));
            return UserMapper.toDomain(saved);
        });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return TenantContext.runCrossTenant(
                () -> jpa.findByEmail(email).map(UserMapper::toDomain));
    }

    @Override
    public Optional<User> findByIdAndInstitutionId(Long id, Long institutionId) {
        return jpa.findByIdAndInstitutionId(id, institutionId).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    // IMPLEMENTACIÓN DEL NUEVO MÉTODO DEL PUERTO
    @Override
    public List<User> findLeaderboardByInstitutionId(Long institutionId) {
        return jpa.findByInstitutionIdOrderByStreakDesc(institutionId)
                .stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toList());
    }
}