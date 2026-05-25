package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.CreateFlashcardRequest;
import com.streakstudy.application.dto.FlashcardDetailResponse;
import com.streakstudy.application.dto.FlashcardResponse;
import com.streakstudy.application.dto.UpdateFlashcardRequest;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.domain.model.Deck;
import com.streakstudy.domain.model.Difficulty;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.repository.DeckRepository;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock FlashcardRepository flashcards;
    @Mock DeckRepository decks;
    @InjectMocks FlashcardService service;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateFlashcardWhenDeckBelongsToCurrentTenant() {
        TenantContext.set(7L);
        when(decks.findByIdAndInstitutionId(3L, 7L)).thenReturn(
            Optional.of(new Deck(3L, 7L, "D", "", Instant.now())));
        when(flashcards.save(any(Flashcard.class))).thenAnswer(inv -> {
            Flashcard f = inv.getArgument(0);
            return new Flashcard(101L, f.institutionId(), f.deckId(), f.question(),
                f.answer(), f.difficulty(), Instant.now());
        });

        FlashcardResponse resp = service.create(new CreateFlashcardRequest(
            3L, "Q?", "A.", Difficulty.MEDIUM));

        assertThat(resp.id()).isEqualTo(101L);
        assertThat(resp.deckId()).isEqualTo(3L);
        assertThat(resp.question()).isEqualTo("Q?");
        assertThat(resp.difficulty()).isEqualTo(Difficulty.MEDIUM);

        ArgumentCaptor<Flashcard> captor = ArgumentCaptor.forClass(Flashcard.class);
        verify(flashcards).save(captor.capture());
        assertThat(captor.getValue().institutionId()).isEqualTo(7L);
    }

    @Test
    void shouldThrowNotFoundWhenCreatingFlashcardOnMissingDeck() {
        TenantContext.set(7L);
        when(decks.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateFlashcardRequest(
                99L, "Q?", "A.", Difficulty.EASY)))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Deck");
        verify(flashcards, never()).save(any());
    }

    @Test
    void shouldThrowTenantViolationWhenCreatingFlashcardWithoutTenantContext() {
        assertThatThrownBy(() -> service.create(new CreateFlashcardRequest(
                1L, "Q?", "A.", Difficulty.MEDIUM)))
            .isInstanceOf(TenantViolationException.class);
        verify(flashcards, never()).save(any());
    }

    @Test
    void shouldListFlashcardsByDeckWithinCurrentTenant() {
        TenantContext.set(7L);
        when(flashcards.findAllByDeckIdAndInstitutionId(3L, 7L)).thenReturn(List.of(
            new Flashcard(1L, 7L, 3L, "Q1", "A1", Difficulty.EASY, Instant.now()),
            new Flashcard(2L, 7L, 3L, "Q2", "A2", Difficulty.HARD, Instant.now())
        ));

        List<FlashcardResponse> result = service.listByDeck(3L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FlashcardResponse::deckId).containsOnly(3L);
        verify(flashcards).findAllByDeckIdAndInstitutionId(3L, 7L);
    }

    @Test
    void shouldReturnFlashcardDetailWhenItExistsInCurrentTenant() {
        TenantContext.set(7L);
        Flashcard f = new Flashcard(5L, 7L, 3L, "Q?", "A.", Difficulty.MEDIUM, Instant.now());
        when(flashcards.findByIdAndInstitutionId(5L, 7L)).thenReturn(Optional.of(f));

        FlashcardDetailResponse resp = service.getById(5L);

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.deckId()).isEqualTo(3L);
    }

    @Test
    void shouldThrowNotFoundWhenGettingMissingFlashcard() {
        TenantContext.set(7L);
        when(flashcards.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Flashcard");
    }

    @Test
    void shouldUpdateFlashcardFieldsWhenItExistsInCurrentTenant() {
        TenantContext.set(7L);
        Flashcard existing = new Flashcard(5L, 7L, 3L, "old Q", "old A",
            Difficulty.EASY, Instant.parse("2025-01-01T00:00:00Z"));
        when(flashcards.findByIdAndInstitutionId(5L, 7L)).thenReturn(Optional.of(existing));
        when(flashcards.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateFlashcardRequest req = new UpdateFlashcardRequest();
        setField(req, "question", "new Q");
        setField(req, "answer", "new A");
        setField(req, "difficulty", Difficulty.HARD);

        FlashcardResponse resp = service.update(5L, req);

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.question()).isEqualTo("new Q");
        assertThat(resp.answer()).isEqualTo("new A");
        assertThat(resp.difficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingFlashcard() {
        TenantContext.set(7L);
        when(flashcards.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        UpdateFlashcardRequest req = new UpdateFlashcardRequest();
        setField(req, "question", "Q");
        setField(req, "answer", "A");
        setField(req, "difficulty", Difficulty.EASY);

        assertThatThrownBy(() -> service.update(99L, req))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Flashcard");
        verify(flashcards, never()).save(any());
    }

    @Test
    void shouldDeleteFlashcardOnlyWithinCurrentTenant() {
        TenantContext.set(7L);
        when(flashcards.findByIdAndInstitutionId(5L, 7L)).thenReturn(
            Optional.of(new Flashcard(5L, 7L, 3L, "Q", "A", Difficulty.EASY, Instant.now())));

        service.delete(5L);

        verify(flashcards).deleteByIdAndInstitutionId(5L, 7L);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingFlashcard() {
        TenantContext.set(7L);
        when(flashcards.findByIdAndInstitutionId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(EntityNotFoundException.class);
        verify(flashcards, never()).deleteByIdAndInstitutionId(any(), any());
    }

    /**
     * {@link UpdateFlashcardRequest} es una clase mutable con setters parciales
     * (no expone setter para {@code difficulty}). Usamos reflexion para los
     * tests.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
