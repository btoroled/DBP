package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.model.Course;
import com.streakstudy.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({InstitutionRepositoryAdapter.class, CourseRepositoryAdapter.class})
class CourseRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired CourseRepositoryAdapter courses;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveFindCountAndDeleteCourseWithinTenant() {
        Long institutionId = institutions.save(com.streakstudy.domain.model.Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(institutionId);

        Course saved = courses.save(Course.newInstance(institutionId, "Calculo", "Integral"));
        List<Course> all = courses.findAllByInstitutionId(institutionId);
        Optional<Course> byId = courses.findByIdAndInstitutionId(saved.id(), institutionId);

        assertThat(saved.id()).isNotNull();
        assertThat(all).hasSize(1);
        assertThat(byId).isPresent();
        assertThat(courses.countByInstitutionId(institutionId)).isEqualTo(1);

        courses.deleteByIdAndInstitutionId(saved.id(), institutionId);

        assertThat(courses.findByIdAndInstitutionId(saved.id(), institutionId)).isEmpty();
    }
}
