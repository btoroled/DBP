package com.streakstudy.application.service;

import java.util.List;

import com.streakstudy.application.dto.FlashcardDetailResponse;
import com.streakstudy.application.dto.UpdateFlashcardRequest;
import com.streakstudy.domain.repository.DeckRepository;
import com.streakstudy.domain.model.Difficulty; // ¡IMPORTANTE: Importamos el Enum del dominio!
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.CreateFlashcardRequest;
import com.streakstudy.application.dto.FlashcardResponse;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Service
public class FlashcardService {

    private final FlashcardRepository flashcards;
    private final DeckRepository decks;

    public FlashcardService(
            FlashcardRepository flashcards,
            DeckRepository decks
    ) {
        this.flashcards = flashcards;
        this.decks = decks;
    }

    @Transactional
    public FlashcardResponse create(CreateFlashcardRequest req) {

        Long tenantId = TenantContext.requireInstitutionId();

        decks.findByIdAndInstitutionId(
                req.deckId(),
                tenantId
        ).orElseThrow(() ->
                new EntityNotFoundException(
                        "Deck",
                        req.deckId().toString()
                )
        );

        // Cambia req.getDifficulty() por esto:
        Difficulty difficultyEnum = req.difficulty();
        Flashcard flashcard = Flashcard.newInstance(
                tenantId,
                req.deckId(),
                req.question(),
                req.answer(),
                difficultyEnum // Ahora sí le pasamos el tipo de dato requerido
        );

        return FlashcardResponse.from(
                flashcards.save(flashcard)
        );
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> listByDeck(Long deckId) {

        Long tenantId = TenantContext.requireInstitutionId();

        return flashcards
                .findAllByDeckIdAndInstitutionId(deckId, tenantId)
                .stream()
                .map(FlashcardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardDetailResponse getById(Long id) {

        Long tenantId = TenantContext.requireInstitutionId();

        return flashcards
                .findByIdAndInstitutionId(id, tenantId)
                .map(FlashcardDetailResponse::from)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Flashcard",
                                id.toString()
                        )
                );
    }

    @Transactional
    public FlashcardResponse update(Long id, UpdateFlashcardRequest req) {

        Long tenantId = TenantContext.requireInstitutionId();

        Flashcard flashcard = flashcards
                .findByIdAndInstitutionId(id, tenantId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Flashcard",
                                id.toString()
                        )
                );

        // CONVERSIÓN EN UPDATE: También nos aseguramos de transformar la dificultad aquí
        // Nota: Si tus DTOs usan records cambias req.getDifficulty() por req.difficulty()
        Difficulty difficultyEnum = req.getDifficulty();
        flashcard.update(
                req.getQuestion(),
                req.getAnswer(),
                difficultyEnum
        );

        return FlashcardResponse.from(
                flashcards.save(flashcard)
        );
    }

    @Transactional
    public void delete(Long id) {

        Long tenantId = TenantContext.requireInstitutionId();

        flashcards.findByIdAndInstitutionId(id, tenantId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Flashcard",
                                id.toString()
                        )
                );

        flashcards.deleteByIdAndInstitutionId(id, tenantId);
    }
}