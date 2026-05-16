package com.streakstudy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.streakstudy.domain.model.Course;
import com.streakstudy.domain.repository.CourseRepository;
import com.streakstudy.infrastructure.persistence.entity.CourseJpa;
import com.streakstudy.infrastructure.persistence.mapper.CourseMapper;
import com.streakstudy.infrastructure.persistence.repository.CourseJpaRepository;

@Component
public class CourseRepositoryAdapter implements CourseRepository {

    private final CourseJpaRepository jpa;

    public CourseRepositoryAdapter(CourseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Course save(Course course) {
        CourseJpa saved = jpa.save(CourseMapper.toJpa(course));
        return CourseMapper.toDomain(saved);
    }

    @Override
    public Optional<Course> findByIdAndInstitutionId(Long id, Long institutionId) {
        return jpa.findByIdAndInstitutionId(id, institutionId).map(CourseMapper::toDomain);
    }

    @Override
    public List<Course> findAllByInstitutionId(Long institutionId) {
        return jpa.findAllByInstitutionId(institutionId).stream()
            .map(CourseMapper::toDomain)
            .toList();
    }

    @Override
    public long countByInstitutionId(Long institutionId) {
        return jpa.countByInstitutionId(institutionId);
    }

    @Override
    public void deleteByIdAndInstitutionId(Long id, Long institutionId) {
        jpa.deleteByIdAndInstitutionIdScoped(id, institutionId);
    }
}
