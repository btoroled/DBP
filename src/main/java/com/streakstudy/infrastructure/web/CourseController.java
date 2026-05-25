package com.streakstudy.infrastructure.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.dto.CourseResponse;
import com.streakstudy.application.dto.CreateCourseRequest;
import com.streakstudy.application.service.CourseService;

import jakarta.validation.Valid;

/**
 * Endpoints de cursos. Matriz de roles (Issue #7):
 * <ul>
 *   <li>{@code POST /courses}: TEACHER, INSTITUTION_ADMIN, SUPER_ADMIN</li>
 *   <li>{@code GET /courses[/{id}]}: cualquier usuario autenticado</li>
 *   <li>{@code DELETE /courses/{id}}: INSTITUTION_ADMIN, SUPER_ADMIN</li>
 * </ul>
 */
@Tag(name = "Courses", description = "Gestión de cursos")
@RestController
@RequestMapping("/api/v1/courses")
@PreAuthorize("isAuthenticated()")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @Operation(summary = "Crear curso")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER','INSTITUTION_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @Operation(summary = "Listar cursos")
    @GetMapping
    public List<CourseResponse> list() {
        return service.listForCurrentTenant();
    }

    @Operation(summary = "Obtener curso por id")
    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return service.getByIdForCurrentTenant(id);
    }

    @Operation(summary = "Eliminar curso")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INSTITUTION_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteByIdForCurrentTenant(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
