package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.Course;
import com.streakstudy.infrastructure.persistence.entity.CourseJpa;

public final class CourseMapper {

    private CourseMapper() { }

    public static Course toDomain(CourseJpa jpa) {
        return new Course(
            jpa.getId(),
            jpa.getInstitutionId(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getCreatedAt()
        );
    }

    public static CourseJpa toJpa(Course domain) {
        CourseJpa jpa = new CourseJpa();
        jpa.setId(domain.id());
        jpa.setInstitutionId(domain.institutionId());
        jpa.setName(domain.name());
        jpa.setDescription(domain.description());
        jpa.setCreatedAt(domain.createdAt());
        return jpa;
    }
}
