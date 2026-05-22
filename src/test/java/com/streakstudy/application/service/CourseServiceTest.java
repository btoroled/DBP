package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.CourseResponse;
import com.streakstudy.application.dto.CreateCourseRequest;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.domain.model.Course;
import com.streakstudy.domain.repository.CourseRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository repo;
    @InjectMocks CourseService service;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldUseInstitutionIdFromTenantContextWhenCreatingCourse() {
        TenantContext.set(42L);
        when(repo.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            return new Course(1L, c.institutionId(), c.name(), c.description(), Instant.now());
        });

        CourseResponse resp = service.create(new CreateCourseRequest("Calculo", "desc"));

        assertThat(resp.institutionId()).isEqualTo(42L);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().institutionId()).isEqualTo(42L);
    }

    @Test
    void shouldThrowTenantViolationWhenCreatingCourseWithoutTenantContext() {
        // No setTenant -> requireInstitutionId debe explotar
        assertThatThrownBy(() -> service.create(new CreateCourseRequest("X", "")))
            .isInstanceOf(TenantViolationException.class)
            .hasMessageContaining("No hay un institutionId");
        verify(repo, never()).save(any());
    }

    @Test
    void shouldFilterCoursesByTenantWhenListingCurrentTenant() {
        TenantContext.set(7L);
        when(repo.findAllByInstitutionId(7L)).thenReturn(List.of(
            new Course(1L, 7L, "A", "", Instant.now()),
            new Course(2L, 7L, "B", "", Instant.now())
        ));

        List<CourseResponse> result = service.listForCurrentTenant();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(c -> assertThat(c.institutionId()).isEqualTo(7L));
        verify(repo).findAllByInstitutionId(7L);
        // CRITICAL: nunca llamamos findAll() global sin tenant
        verify(repo, never()).findByIdAndInstitutionId(any(), eq(0L));
    }

    @Test
    void shouldThrowNotFoundWhenCourseIsMissingInCurrentTenant() {
        TenantContext.set(7L);
        when(repo.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForCurrentTenant(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Course")
            .hasMessageContaining("tenant=7");
    }

    @Test
    void shouldReturnCourseFromCurrentTenantWhenIdExists() {
        TenantContext.set(7L);
        Course owned = new Course(5L, 7L, "Curso", "", Instant.now());
        when(repo.findByIdAndInstitutionId(5L, 7L)).thenReturn(Optional.of(owned));

        CourseResponse resp = service.getByIdForCurrentTenant(5L);

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.institutionId()).isEqualTo(7L);
    }

    @Test
    void shouldDeleteCourseOnlyWithinCurrentTenant() {
        TenantContext.set(7L);
        when(repo.findByIdAndInstitutionId(5L, 7L)).thenReturn(
            Optional.of(new Course(5L, 7L, "X", "", Instant.now())));

        service.deleteByIdForCurrentTenant(5L);

        verify(repo).deleteByIdAndInstitutionId(5L, 7L);
    }
}
