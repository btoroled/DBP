package com.streakstudy.infrastructure.web;

import java.util.List;

import com.streakstudy.application.dto.FlashcardDetailResponse;
import com.streakstudy.application.dto.UpdateFlashcardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.streakstudy.application.dto.CreateFlashcardRequest;
import com.streakstudy.application.dto.FlashcardResponse;
import com.streakstudy.application.service.FlashcardService;

import jakarta.validation.Valid;

/**
 * Endpoints de flashcards. Matriz de roles (Issue #7):
 * <ul>
 *   <li>Lectura ({@code GET}): cualquier usuario autenticado.</li>
 *   <li>Escritura ({@code POST}, {@code PUT}, {@code DELETE}): STUDENT,
 *       TEACHER, INSTITUTION_ADMIN, SUPER_ADMIN. La autoria a nivel de
 *       recurso se valida en el {@code FlashcardService} via {@code TenantContext}.</li>
 * </ul>
 */
@Tag(name = "Flashcards")
@RestController
@RequestMapping("/api/v1/flashcards")
@Validated
@PreAuthorize("isAuthenticated()")
public class FlashcardController {

    private static final String FLASHCARD_WRITERS =
        "hasAnyAuthority('STUDENT','TEACHER','INSTITUTION_ADMIN','SUPER_ADMIN')";

    private final FlashcardService flashcardService;

    public FlashcardController(
            FlashcardService flashcardService
    ) {
        this.flashcardService = flashcardService;
    }

    @Operation(summary = "Crear flashcard")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(FLASHCARD_WRITERS)
    public FlashcardResponse create(
            @Valid @RequestBody CreateFlashcardRequest req
    ) {
        return flashcardService.create(req);
    }

    @Operation(summary = "Listar flashcards por deck")
    @GetMapping("/deck/{deckId}")
    public List<FlashcardResponse> listByDeck(
            @PathVariable Long deckId
    ) {
        return flashcardService.listByDeck(deckId);
    }

    @Operation(summary = "Obtener flashcard por id")
    @GetMapping("/{id}")
    public FlashcardDetailResponse getById(
            @PathVariable Long id
    ) {
        return flashcardService.getById(id);
    }

    @Operation(summary = "Actualizar flashcard")
    @PutMapping("/{flashcardId}")
    @PreAuthorize(FLASHCARD_WRITERS)
    public ResponseEntity<FlashcardResponse> updateFlashcard(
            @PathVariable Long flashcardId,
            @Valid @RequestBody UpdateFlashcardRequest request
    ) {

        FlashcardResponse response =
                flashcardService.update(flashcardId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar flashcard")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(FLASHCARD_WRITERS)
    public void delete(
            @PathVariable Long id
    ) {
        flashcardService.delete(id);
    }
}
