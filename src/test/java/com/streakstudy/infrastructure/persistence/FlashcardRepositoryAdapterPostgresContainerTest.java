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
import com.streakstudy.domain.model.Difficulty;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.DeckRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.FlashcardRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    InstitutionRepositoryAdapter.class,
    DeckRepositoryAdapter.class,
    FlashcardRepositoryAdapter.class
})
class FlashcardRepositoryAdapterPostgresContainerTest {

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
    @Autowired FlashcardRepositoryAdapter flashcards;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldPersistAndRetrieveFlashcardByIdWhenUsingPostgres() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Long deckId = decks.save(Deck.newInstance(inst, "D", "")).id();

        Flashcard saved = flashcards.save(
            Flashcard.newInstance(inst, deckId, "¿Qué es DIP?", "Dependency Inversion", Difficulty.HARD));

        Optional<Flashcard> found = flashcards.findByIdAndInstitutionId(saved.id(), inst);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().question()).isEqualTo("¿Qué es DIP?");
        assertThat(found.get().answer()).isEqualTo("Dependency Inversion");
        assertThat(found.get().difficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    void shouldGroupFlashcardsByDeckWithinSameTenantOnPostgres() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Long deck1 = decks.save(Deck.newInstance(inst, "D1", "")).id();
        Long deck2 = decks.save(Deck.newInstance(inst, "D2", "")).id();

        flashcards.save(Flashcard.newInstance(inst, deck1, "Q1", "A1", Difficulty.EASY));
        flashcards.save(Flashcard.newInstance(inst, deck1, "Q2", "A2", Difficulty.MEDIUM));
        flashcards.save(Flashcard.newInstance(inst, deck1, "Q3", "A3", Difficulty.HARD));
        flashcards.save(Flashcard.newInstance(inst, deck2, "Q4", "A4", Difficulty.EASY));

        List<Flashcard> deck1Cards = flashcards.findAllByDeckIdAndInstitutionId(deck1, inst);
        List<Flashcard> deck2Cards = flashcards.findAllByDeckIdAndInstitutionId(deck2, inst);

        assertThat(deck1Cards).hasSize(3).extracting(Flashcard::deckId).containsOnly(deck1);
        assertThat(deck2Cards).hasSize(1).extracting(Flashcard::deckId).containsOnly(deck2);
    }

    @Test
    void shouldIsolateFlashcardLookupByTenantOnPostgres() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        Long deck = decks.save(Deck.newInstance(utec, "D", "")).id();
        Flashcard saved = flashcards.save(
            Flashcard.newInstance(utec, deck, "Q", "A", Difficulty.MEDIUM));

        assertThat(flashcards.findByIdAndInstitutionId(saved.id(), pucp)).isEmpty();
        assertThat(flashcards.findByIdAndInstitutionId(saved.id(), utec)).isPresent();
    }

    @Test
    void shouldRejectForeignTenantDeleteOnPostgres() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        Long deck = decks.save(Deck.newInstance(utec, "D", "")).id();
        Flashcard saved = flashcards.save(
            Flashcard.newInstance(utec, deck, "Q", "A", Difficulty.EASY));

        flashcards.deleteByIdAndInstitutionId(saved.id(), pucp);
        assertThat(flashcards.findByIdAndInstitutionId(saved.id(), utec)).isPresent();

        flashcards.deleteByIdAndInstitutionId(saved.id(), utec);
        assertThat(flashcards.findByIdAndInstitutionId(saved.id(), utec)).isEmpty();
    }
}
