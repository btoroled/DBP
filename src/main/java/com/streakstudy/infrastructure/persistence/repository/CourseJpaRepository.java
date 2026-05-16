package com.streakstudy.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.streakstudy.infrastructure.persistence.entity.CourseJpa;

public interface CourseJpaRepository extends JpaRepository<CourseJpa, Long> {
    Optional<CourseJpa> findByIdAndInstitutionId(Long id, Long institutionId);
    List<CourseJpa> findAllByInstitutionId(Long institutionId);
    long countByInstitutionId(Long institutionId);

    @Modifying
    @Query("delete from CourseJpa c where c.id = :id and c.institutionId = :institutionId")
    int deleteByIdAndInstitutionIdScoped(Long id, Long institutionId);
}
