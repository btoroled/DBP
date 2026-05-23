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

import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.UserRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({InstitutionRepositoryAdapter.class, UserRepositoryAdapter.class})
class UserRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired UserRepositoryAdapter users;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndFindUserByEmailAndTenant() {
        Long institutionId = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(institutionId);
        User saved = users.save(student(null, institutionId, "alice@test.com", 50, 4, LocalDate.now().minusDays(1), 1));

        Optional<User> byEmail = users.findByEmail("alice@test.com");
        Optional<User> byTenant = users.findByIdAndInstitutionId(saved.id(), institutionId);

        assertThat(saved.id()).isNotNull();
        assertThat(byEmail).isPresent();
        assertThat(byTenant).isPresent();
        assertThat(users.existsByEmail("alice@test.com")).isTrue();
    }

    @Test
    void shouldReturnInactiveUsersAndTopStudentsWithinTenant() {
        Long institutionId = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long otherInstitutionId = institutions.save(Institution.newInstance("PUCP", "pucp")).id();
        TenantContext.set(institutionId);
        users.save(student(null, institutionId, "ana@test.com", 90, 3, LocalDate.now().minusDays(5), 0));
        users.save(student(null, institutionId, "beto@test.com", 120, 5, LocalDate.now(), 0));
        TenantContext.set(otherInstitutionId);
        users.save(student(null, otherInstitutionId, "cora@test.com", 300, 8, LocalDate.now().minusDays(7), 0));

        List<User> inactiveUsers = users.findAllInactiveSince(LocalDate.now().minusDays(1), institutionId);
        List<User> topStudents = users.findTopStudentsByXp(institutionId, 2);

        assertThat(inactiveUsers).extracting(User::email).containsExactly("ana@test.com");
        assertThat(topStudents).hasSize(2);
        assertThat(topStudents.get(0).email()).isEqualTo("beto@test.com");
        assertThat(topStudents).allSatisfy(user -> assertThat(user.institutionId()).isEqualTo(institutionId));
    }

    private User student(Long id, Long institutionId, String email, int xp, int streak, LocalDate lastActiveDate, int freezes) {
        return new User(
            id,
            institutionId,
            email,
            "HASH",
            email,
            UserRole.STUDENT,
            Instant.now(),
            xp,
            streak,
            lastActiveDate,
            freezes,
            Set.of(Badge.STREAK_STARTER)
        );
    }
}
