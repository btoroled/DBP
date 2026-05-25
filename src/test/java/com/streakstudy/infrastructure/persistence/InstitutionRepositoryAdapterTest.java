package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import(InstitutionRepositoryAdapter.class)
class InstitutionRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter repository;

    @Test
    void shouldSaveAndFindInstitutionByIdAndCode() {
        Institution saved = repository.save(Institution.newInstance("UTEC", "utec"));

        Optional<Institution> byId = repository.findById(saved.id());
        Optional<Institution> byCode = repository.findByCode("utec");

        assertThat(saved.id()).isNotNull();
        assertThat(byId).isPresent();
        assertThat(byCode).isPresent();
        assertThat(repository.existsByCode("utec")).isTrue();
    }

    @Test
    void shouldSaveInstitutionCrossTenantEvenWhenTenantContextExists() {
        TenantContext.set(99L);

        Institution saved = repository.save(Institution.newInstance("PUCP", "pucp"));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findByCode("pucp")).isPresent();
        TenantContext.clear();
    }
}
