package com.streakstudy.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
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
    public List<User> findAllInactiveSince(LocalDate thresholdDate, Long institutionId) {
        List<UserJpa> entities = jpa.findAllInactiveSince(thresholdDate, institutionId);

        // resultado en new java.util.ArrayList para que sea modificable
        return new java.util.ArrayList<>(entities.stream()
                .map(entity -> new User(
                        entity.getId(),
                        entity.getInstitutionId(),
                        entity.getEmail(),
                        entity.getPasswordHash(),
                        entity.getFullName(),
                        entity.getRole(),
                        entity.getCreatedAt(),
                        entity.getXp(),
                        entity.getCurrentStreak(),
                        entity.getLastActiveDate(),
                        entity.getStreakFreezes(),
                        entity.getBadges()
                ))
                .toList());
    }

    @Override
    public User save(User user) {
        UserJpa entity = user.id() != null ? jpa.findById(user.id()).orElse(new UserJpa()) : new UserJpa();

        entity.setId(user.id());
        entity.setInstitutionId(user.institutionId());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setFullName(user.fullName());
        entity.setRole(user.role());
        entity.setCreatedAt(user.createdAt());
        entity.setXp(user.xp());
        entity.setCurrentStreak(user.currentStreak());
        entity.setLastActiveDate(user.lastActiveDate());
        entity.setStreakFreezes(user.streakFreezes());
        entity.setBadges(user.badges());

        entity.setBadges(new java.util.HashSet<>(user.badges()));
        UserJpa savedEntity = jpa.save(entity);

        return new User(
                savedEntity.getId(),
                savedEntity.getInstitutionId(),
                savedEntity.getEmail(),
                savedEntity.getPasswordHash(),
                savedEntity.getFullName(),
                savedEntity.getRole(),
                savedEntity.getCreatedAt(),
                savedEntity.getXp(),
                savedEntity.getCurrentStreak(),
                savedEntity.getLastActiveDate(),
                savedEntity.getStreakFreezes(),
                savedEntity.getBadges()
        );
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

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public List<User> findLeaderboardByInstitutionId(Long institutionId) {
        return jpa.findLeaderboardByInstitutionId(institutionId)
                .stream()
                .map(UserMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<User> findTopStudentsByXp(Long institutionId, int topN) {
        List<UserJpa> entities = jpa.findTopUsersByXp(institutionId, Limit.of(topN));

        return new java.util.ArrayList<>(entities.stream()
                .map(entity -> new User(
                        entity.getId(),
                        entity.getInstitutionId(),
                        entity.getEmail(),
                        entity.getPasswordHash(),
                        entity.getFullName(),
                        entity.getRole(),
                        entity.getCreatedAt(),
                        entity.getXp(),
                        entity.getCurrentStreak(),
                        entity.getLastActiveDate(),
                        entity.getStreakFreezes(),
                        entity.getBadges()
                ))
                .toList());
    }

}
