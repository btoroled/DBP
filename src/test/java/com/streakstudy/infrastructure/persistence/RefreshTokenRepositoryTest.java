package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.UserRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.entity.RefreshTokenJpa;
import com.streakstudy.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({InstitutionRepositoryAdapter.class, UserRepositoryAdapter.class})
class RefreshTokenRepositoryTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired UserRepositoryAdapter users;
    @Autowired RefreshTokenJpaRepository refreshTokens;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndFindByTokenHash() {
        Long institutionId = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(institutionId);
        User user = users.save(new User(
            null,
            institutionId,
            "alice@test.com",
            "HASH",
            "Alice",
            UserRole.STUDENT,
            Instant.now(),
            0,
            0,
            null,
            0,
            Set.of()
        ));

        RefreshTokenJpa token = new RefreshTokenJpa();
        token.setUserId(user.id());
        token.setTokenHash("hash-123");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRevoked(false);
        refreshTokens.save(token);

        RefreshTokenJpa found = refreshTokens.findByTokenHash("hash-123").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getUserId()).isEqualTo(user.id());
        assertThat(found.isRevoked()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
    }
}
