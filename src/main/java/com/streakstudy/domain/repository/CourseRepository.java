package com.streakstudy.domain.repository;

import java.util.List;
import java.util.Optional;

import com.streakstudy.domain.model.Course;

/**
 * Puerto de persistencia para Course. <b>Tenant-aware</b>: todas las operaciones
 * de lectura y escritura reciben explicitamente el {@code institutionId} para
 * garantizar el aislamiento incluso si {@code TenantContext} fallara.
 */
public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findByIdAndInstitutionId(Long id, Long institutionId);

    List<Course> findAllByInstitutionId(Long institutionId);

    long countByInstitutionId(Long institutionId);

    void deleteByIdAndInstitutionId(Long id, Long institutionId);
}
