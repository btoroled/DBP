package com.streakstudy.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streakstudy.infrastructure.persistence.entity.RefreshTokenJpa;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpa, Long> {

    Optional<RefreshTokenJpa> findByTokenHash(String tokenHash);
}
