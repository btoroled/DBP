package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

import com.streakstudy.application.dto.CourseResponse;
import com.streakstudy.application.dto.CreateCourseRequest;
import com.streakstudy.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InstitutionRepositoryAdapter.class, CourseRepositoryAdapter.class, CourseService.class})
class CourseServicePostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("streakstudy_service_test")
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
    @Autowired CourseService courseService;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateAndListCoursesUsingRealPostgresContainer() {
        Long institutionId = institutions.save(com.streakstudy.domain.model.Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(institutionId);

        CourseResponse created = courseService.create(new CreateCourseRequest("Calculo", "Integral"));
        List<CourseResponse> listed = courseService.listForCurrentTenant();

        assertThat(created.id()).isNotNull();
        assertThat(created.institutionId()).isEqualTo(institutionId);
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).name()).isEqualTo("Calculo");
    }

    @Test
    void shouldDeleteCourseUsingRealPostgresContainer() {
        Long institutionId = institutions.save(com.streakstudy.domain.model.Institution.newInstance("PUCP", "pucp")).id();
        TenantContext.set(institutionId);
        CourseResponse created = courseService.create(new CreateCourseRequest("Fisica", "Mecánica"));

        courseService.deleteByIdForCurrentTenant(created.id());

        assertThat(courseService.listForCurrentTenant()).isEmpty();
    }
}
