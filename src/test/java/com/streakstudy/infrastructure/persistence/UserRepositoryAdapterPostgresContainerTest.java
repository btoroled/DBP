package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.UserRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InstitutionRepositoryAdapter.class, UserRepositoryAdapter.class})
class UserRepositoryAdapterPostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("streakstudy_test")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired UserRepositoryAdapter users;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldPersistAndQueryUsersAgainstRealPostgresContainer() {
        Long institutionId = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(institutionId);
        User saved = users.save(student(null, institutionId, "alice@test.com", 75, 4, LocalDate.now().minusDays(2), 1));

        Optional<User> byEmail = users.findByEmail("alice@test.com");
        List<User> ranking = users.findTopStudentsByXp(institutionId, 5);

        assertThat(saved.id()).isNotNull();
        assertThat(byEmail).isPresent();
        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).email()).isEqualTo("alice@test.com");
    }

    private User student(Long id, Long institutionId, String email, int xp, int streak, LocalDate lastActiveDate, int freezes) {
        return new User(
            id,
            institutionId,
            email,
            "HASH",
            "Alice",
            UserRole.STUDENT,
            Instant.now(),
            xp,
            streak,
            lastActiveDate,
            freezes,
            Set.of()
        );
    }
}
