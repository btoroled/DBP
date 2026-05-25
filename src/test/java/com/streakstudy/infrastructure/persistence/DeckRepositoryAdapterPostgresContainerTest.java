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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.DeckRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InstitutionRepositoryAdapter.class, DeckRepositoryAdapter.class})
class DeckRepositoryAdapterPostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("streakstudy_test")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired DeckRepositoryAdapter decks;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldPersistAndRetrieveDeckByIdWhenUsingPostgres() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);

        Deck saved = decks.save(Deck.newInstance(inst, "Calculo", "Calc I"));

        Optional<Deck> found = decks.findByIdAndInstitutionId(saved.id(), inst);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Calculo");
        assertThat(found.get().description()).isEqualTo("Calc I");
    }

    @Test
    void shouldIsolateDecksByTenantWhenQueryingAgainstPostgres() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        decks.save(Deck.newInstance(utec, "Calc UTEC", ""));
        decks.save(Deck.newInstance(utec, "Alg UTEC", ""));

        TenantContext.set(pucp);
        decks.save(Deck.newInstance(pucp, "Calc PUCP", ""));

        TenantContext.set(utec);
        List<Deck> utecDecks = decks.findAllByInstitutionId(utec);
        List<Deck> pucpDecks = decks.findAllByInstitutionId(pucp);

        assertThat(utecDecks).hasSize(2).extracting(Deck::institutionId).containsOnly(utec);
        assertThat(pucpDecks).hasSize(1).extracting(Deck::institutionId).containsOnly(pucp);
    }

    @Test
    void shouldCountDecksScopedToTenantOnPostgres() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        decks.save(Deck.newInstance(utec, "A", ""));
        decks.save(Deck.newInstance(utec, "B", ""));
        decks.save(Deck.newInstance(utec, "C", ""));

        TenantContext.set(pucp);
        decks.save(Deck.newInstance(pucp, "X", ""));

        assertThat(decks.countByInstitutionId(utec)).isEqualTo(3);
        assertThat(decks.countByInstitutionId(pucp)).isEqualTo(1);
    }

    @Test
    void shouldNotDeleteDeckFromForeignTenantOnPostgres() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        Deck saved = decks.save(Deck.newInstance(utec, "Tenant-locked", ""));

        decks.deleteByIdAndInstitutionId(saved.id(), pucp);

        assertThat(decks.findByIdAndInstitutionId(saved.id(), utec)).isPresent();

        decks.deleteByIdAndInstitutionId(saved.id(), utec);

        assertThat(decks.findByIdAndInstitutionId(saved.id(), utec)).isEmpty();
    }
}
