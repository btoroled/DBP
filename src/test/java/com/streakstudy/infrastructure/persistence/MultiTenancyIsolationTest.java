package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.entity.CourseJpa;
import com.streakstudy.infrastructure.persistence.entity.InstitutionJpa;
import com.streakstudy.infrastructure.persistence.repository.CourseJpaRepository;
import com.streakstudy.infrastructure.persistence.repository.InstitutionJpaRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

/**
 * <h1>Test crítico de aislamiento multi-tenant — TENANT-001</h1>
 *
 * <p>Este test es el que <b>verifica el criterio de aceptacion</b>
 * "usuarios solo ven datos de su institucion, no hay fuga de datos":</p>
 * <ol>
 *   <li>Crea instituciones A y B.</li>
 *   <li>Inserta cursos en cada una.</li>
 *   <li>Con {@code TenantContext = A}, lista cursos: solo deben aparecer los de A.</li>
 *   <li>Con {@code TenantContext = A}, intenta {@code findById} de un curso de B:
 *       debe devolver {@code Optional.empty()}.</li>
 *   <li>Intentar persistir un curso con {@code institution_id} de B mientras el
 *       contexto es A debe lanzar {@link TenantViolationException} (defensa del listener).</li>
 * </ol>
 *
 * <p>Usamos H2 en modo PostgreSQL para que el test sea rapido pero realista.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({
    InstitutionRepositoryAdapter.class,
    CourseRepositoryAdapter.class
})
class MultiTenancyIsolationTest {

    @Autowired CourseJpaRepository courseJpa;
    @Autowired InstitutionJpaRepository institutionJpa;
    @Autowired CourseRepositoryAdapter courseAdapter;

    private Long instAId;
    private Long instBId;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private void seedTwoInstitutionsWithCourses() {
        TenantContext.runCrossTenant(() -> {
            InstitutionJpa a = new InstitutionJpa();
            a.setCode("inst-a"); a.setName("Institucion A"); a.setActive(true);
            instAId = institutionJpa.save(a).getId();

            InstitutionJpa b = new InstitutionJpa();
            b.setCode("inst-b"); b.setName("Institucion B"); b.setActive(true);
            instBId = institutionJpa.save(b).getId();

            CourseJpa cA1 = course("Calculo A", instAId);
            CourseJpa cA2 = course("Algebra A", instAId);
            CourseJpa cB1 = course("Calculo B", instBId);
            courseJpa.saveAll(List.of(cA1, cA2, cB1));
        });
    }

    private CourseJpa course(String name, Long instId) {
        CourseJpa c = new CourseJpa();
        c.setName(name);
        c.setInstitutionId(instId);
        return c;
    }

    @Test
    @DisplayName("Con TenantContext=A, listar cursos devuelve solo los de A")
    void listFiltraPorTenantA() {
        seedTwoInstitutionsWithCourses();

        TenantContext.set(instAId);
        var cursos = courseAdapter.findAllByInstitutionId(instAId);

        assertThat(cursos).hasSize(2);
        assertThat(cursos).allSatisfy(c -> assertThat(c.institutionId()).isEqualTo(instAId));
        assertThat(cursos).extracting("name")
            .containsExactlyInAnyOrder("Calculo A", "Algebra A");
    }

    @Test
    @DisplayName("Con TenantContext=B, listar cursos devuelve solo los de B")
    void listFiltraPorTenantB() {
        seedTwoInstitutionsWithCourses();

        TenantContext.set(instBId);
        var cursos = courseAdapter.findAllByInstitutionId(instBId);

        assertThat(cursos).hasSize(1);
        assertThat(cursos.get(0).name()).isEqualTo("Calculo B");
    }

    @Test
    @DisplayName("findById de curso de B con TenantContext=A devuelve empty (no fuga)")
    void findByIdNoEscapaEntreTenants() {
        seedTwoInstitutionsWithCourses();

        Long cursoDeBId = TenantContext.runCrossTenant(() ->
            courseJpa.findAll().stream()
                .filter(c -> c.getInstitutionId().equals(instBId))
                .findFirst().orElseThrow().getId());

        TenantContext.set(instAId);
        Optional<com.streakstudy.domain.model.Course> result =
            courseAdapter.findByIdAndInstitutionId(cursoDeBId, instAId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Persistir entidad con institution_id de B desde contexto=A lanza TenantViolationException")
    void listenerBloqueaEscrituraConTenantIncorrecto() {
        seedTwoInstitutionsWithCourses();

        TenantContext.set(instAId);

        CourseJpa intruso = new CourseJpa();
        intruso.setName("Curso intruso");
        intruso.setInstitutionId(instBId); // <-- intento de fuga

        assertThatThrownBy(() -> courseJpa.saveAndFlush(intruso))
            .satisfiesAnyOf(
                ex -> assertThat(ex).isInstanceOf(TenantViolationException.class),
                ex -> assertThat(ex.getCause()).isInstanceOf(TenantViolationException.class),
                ex -> assertThat(ex).hasMessageContaining("tenant"),
                ex -> assertThat(rootCause(ex)).isInstanceOf(TenantViolationException.class)
            );
    }

    @Test
    @DisplayName("Persistir sin institution_id pero con TenantContext=A asigna A automaticamente")
    void listenerAsignaTenantCuandoFalta() {
        seedTwoInstitutionsWithCourses();

        TenantContext.set(instAId);

        CourseJpa nuevo = new CourseJpa();
        nuevo.setName("Curso nuevo");
        // institution_id NO seteado: el listener debe asignarlo desde el contexto

        CourseJpa guardado = courseJpa.saveAndFlush(nuevo);

        assertThat(guardado.getInstitutionId()).isEqualTo(instAId);
    }

    @Test
    @DisplayName("countByInstitutionId no cuenta cursos del otro tenant")
    void countByTenantNoMezcla() {
        seedTwoInstitutionsWithCourses();

        TenantContext.set(instAId);
        assertThat(courseAdapter.countByInstitutionId(instAId)).isEqualTo(2);
        assertThat(courseAdapter.countByInstitutionId(instBId)).isEqualTo(1);
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }
}
