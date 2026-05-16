package com.streakstudy.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.CourseResponse;
import com.streakstudy.application.dto.CreateCourseRequest;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.domain.model.Course;
import com.streakstudy.domain.repository.CourseRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

/**
 * Servicio tenant-aware. Lee el tenant del {@link TenantContext} (poblado
 * por el {@code JwtAuthenticationFilter}) y lo propaga explicitamente a todas
 * las queries.
 *
 * <p>La cadena de defensa contra fugas de datos:</p>
 * <ol>
 *   <li>{@link TenantContext#requireInstitutionId()} aborta si no hay tenant.</li>
 *   <li>Las queries siempre incluyen el {@code institutionId} (defensa primaria).</li>
 *   <li>El JPA listener valida que el {@code institution_id} de la entidad
 *       coincida con el contexto al persistir (defensa en escritura).</li>
 * </ol>
 */
@Service
public class CourseService {

    private final CourseRepository courses;

    public CourseService(CourseRepository courses) {
        this.courses = courses;
    }

    @Transactional
    public CourseResponse create(CreateCourseRequest req) {
        Long tenantId = TenantContext.requireInstitutionId();
        Course toSave = Course.newInstance(tenantId, req.name(), req.description());
        return CourseResponse.from(courses.save(toSave));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listForCurrentTenant() {
        Long tenantId = TenantContext.requireInstitutionId();
        return courses.findAllByInstitutionId(tenantId).stream()
            .map(CourseResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getByIdForCurrentTenant(Long id) {
        Long tenantId = TenantContext.requireInstitutionId();
        return courses.findByIdAndInstitutionId(id, tenantId)
            .map(CourseResponse::from)
            .orElseThrow(() -> tenantScopedNotFound(id, tenantId));
    }

    @Transactional
    public void deleteByIdForCurrentTenant(Long id) {
        Long tenantId = TenantContext.requireInstitutionId();
        // findFirst ensures we throw a clear 404 rather than silently no-oping
        courses.findByIdAndInstitutionId(id, tenantId)
            .orElseThrow(() -> tenantScopedNotFound(id, tenantId));
        courses.deleteByIdAndInstitutionId(id, tenantId);
    }

    /**
     * Devolvemos 404 (no 403) intencionadamente: un atacante no debe poder
     * inferir la existencia de un recurso en otro tenant. Para devolver 403
     * en su lugar, habria que hacer un {@code findById} cross-tenant y, si
     * existe pero en otra institucion, lanzar {@link TenantViolationException}.
     */
    private RuntimeException tenantScopedNotFound(Long id, Long tenantId) {
        return new EntityNotFoundException("Course", "%s (tenant=%s)".formatted(id, tenantId));
    }
}
