package com.streakstudy.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Optional;

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
        // El registro y la actualizacion por administrador son cross-tenant a nivel
        // de adapter: el institutionId viaja en la propia entidad y el listener
        // lo valida (en cross-tenant=false) o lo respeta (en cross-tenant=true).
        return TenantContext.runCrossTenant(() -> {
            UserJpa saved = jpa.save(UserMapper.toJpa(user));
            return UserMapper.toDomain(saved);
        });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        // Lookup cross-tenant intencional para login.
        return TenantContext.runCrossTenant(
            () -> jpa.findByEmail(email).map(UserMapper::toDomain));
    }

    @Override
    public Optional<User> findByIdAndInstitutionId(Long id, Long institutionId) {
        return jpa.findByIdAndInstitutionId(id, institutionId).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return TenantContext.runCrossTenant(() -> jpa.existsByEmail(email));
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public int consumeStreakFreezes(LocalDate thresholdDate) {
        return jpa.consumeStreakFreezes(thresholdDate);
    }

    @Override
    public int resetUnprotectedStreaks(LocalDate thresholdDate) {
        return jpa.resetUnprotectedStreaks(thresholdDate);
    }
}
