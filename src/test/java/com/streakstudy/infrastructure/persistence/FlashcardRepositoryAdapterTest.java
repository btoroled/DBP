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

import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.model.Difficulty;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.DeckRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.FlashcardRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({
    InstitutionRepositoryAdapter.class,
    DeckRepositoryAdapter.class,
    FlashcardRepositoryAdapter.class
})
class FlashcardRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired DeckRepositoryAdapter decks;
    @Autowired FlashcardRepositoryAdapter flashcards;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndAssignIdAndCreatedAtWhenInsertingFlashcard() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Long deckId = decks.save(Deck.newInstance(inst, "D", "")).id();

        Flashcard saved = flashcards.save(
            Flashcard.newInstance(inst, deckId, "Q1?", "A1.", Difficulty.MEDIUM));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.deckId()).isEqualTo(deckId);
        assertThat(saved.difficulty()).isEqualTo(Difficulty.MEDIUM);
    }

    @Test
    void shouldListFlashcardsByDeckWithinSameTenantWhenQuerying() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Long deck1 = decks.save(Deck.newInstance(inst, "D1", "")).id();
        Long deck2 = decks.save(Deck.newInstance(inst, "D2", "")).id();

        flashcards.save(Flashcard.newInstance(inst, deck1, "Q1", "A1", Difficulty.EASY));
        flashcards.save(Flashcard.newInstance(inst, deck1, "Q2", "A2", Difficulty.HARD));
        flashcards.save(Flashcard.newInstance(inst, deck2, "Q3", "A3", Difficulty.MEDIUM));

        List<Flashcard> result = flashcards.findAllByDeckIdAndInstitutionId(deck1, inst);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Flashcard::deckId).containsOnly(deck1);
    }

    @Test
    void shouldReturnEmptyWhenFindingFlashcardAcrossTenants() {
        Long utec = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        Long pucp = institutions.save(Institution.newInstance("PUCP", "pucp")).id();

        TenantContext.set(utec);
        Long deck = decks.save(Deck.newInstance(utec, "D", "")).id();
        Flashcard saved = flashcards.save(
            Flashcard.newInstance(utec, deck, "Q?", "A.", Difficulty.MEDIUM));

        Optional<Flashcard> crossTenant = flashcards.findByIdAndInstitutionId(saved.id(), pucp);
        Optional<Flashcard> sameTenant = flashcards.findByIdAndInstitutionId(saved.id(), utec);

        assertThat(crossTenant).isEmpty();
        assertThat(sameTenant).isPresent();
    }

    @Test
    void shouldDeleteFlashcardOnlyForCorrectTenant() {
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
