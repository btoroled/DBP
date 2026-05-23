package com.streakstudy.application.service;

import java.util.List;

import com.streakstudy.application.dto.UpdateFlashcardRequest;
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

    public FlashcardService(FlashcardRepository flashcards) {
        this.flashcards = flashcards;
    }

    @Transactional
    public FlashcardResponse create(CreateFlashcardRequest req) {

        Long tenantId = TenantContext.requireInstitutionId();

        Flashcard flashcard = Flashcard.newInstance(
                tenantId,
                req.deckId(),
                req.question(),
                req.answer()
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
    public FlashcardResponse getById(Long id) {

        Long tenantId = TenantContext.requireInstitutionId();

        return flashcards
                .findByIdAndInstitutionId(id, tenantId)
                .map(FlashcardResponse::from)
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

        flashcard.update(
                req.getQuestion(),
                req.getAnswer()
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