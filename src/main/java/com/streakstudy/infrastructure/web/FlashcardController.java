package com.streakstudy.infrastructure.web;

import java.util.List;

import com.streakstudy.application.dto.FlashcardDetailResponse;
import com.streakstudy.application.dto.UpdateFlashcardRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.streakstudy.application.dto.CreateFlashcardRequest;
import com.streakstudy.application.dto.FlashcardResponse;
import com.streakstudy.application.service.FlashcardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/flashcards")
@Validated
public class FlashcardController {

    private final FlashcardService flashcardService;

    public FlashcardController(
            FlashcardService flashcardService
    ) {
        this.flashcardService = flashcardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardResponse create(
            @Valid @RequestBody CreateFlashcardRequest req
    ) {
        return flashcardService.create(req);
    }

    @GetMapping("/deck/{deckId}")
    public List<FlashcardResponse> listByDeck(
            @PathVariable Long deckId
    ) {
        return flashcardService.listByDeck(deckId);
    }

    @GetMapping("/{id}")
    public FlashcardDetailResponse getById(
            @PathVariable Long id
    ) {
        return flashcardService.getById(id);
    }

    @PutMapping("/{flashcardId}")
    public ResponseEntity<FlashcardResponse> updateFlashcard(
            @PathVariable Long flashcardId,
            @Valid @RequestBody UpdateFlashcardRequest request
    ) {

        FlashcardResponse response =
                flashcardService.update(flashcardId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        flashcardService.delete(id);
    }
}