package com.streakstudy.infrastructure.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.streakstudy.application.dto.CreateDeckRequest;
import com.streakstudy.application.dto.DeckResponse;
import com.streakstudy.application.dto.UpdateDeckRequest;
import com.streakstudy.application.service.DeckService;

import jakarta.validation.Valid;

/**
 * Endpoints de mazos (decks). Matriz de roles (Issue #7):
 * <ul>
 *   <li>Lectura ({@code GET}): cualquier usuario autenticado.</li>
 *   <li>Escritura ({@code POST}, {@code PUT}, {@code DELETE}): STUDENT,
 *       TEACHER, INSTITUTION_ADMIN, SUPER_ADMIN. La autoria a nivel de
 *       recurso se valida en el {@code DeckService} via {@code TenantContext}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/decks")
@Validated
@PreAuthorize("isAuthenticated()")
public class DeckController {

    private static final String DECK_WRITERS =
        "hasAnyAuthority('STUDENT','TEACHER','INSTITUTION_ADMIN','SUPER_ADMIN')";

    private final DeckService decks;

    public DeckController(DeckService decks) {
        this.decks = decks;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(DECK_WRITERS)
    public DeckResponse create(
            @Valid @RequestBody CreateDeckRequest req
    ) {

        return decks.create(req);
    }

    @GetMapping
    public List<DeckResponse> list() {

        return decks.listForCurrentTenant();
    }

    @GetMapping("/{id}")
    public DeckResponse getById(
            @PathVariable Long id
    ) {

        return decks.getByIdForCurrentTenant(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(DECK_WRITERS)
    public DeckResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeckRequest req
    ) {

        return decks.updateForCurrentTenant(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(DECK_WRITERS)
    public void delete(
            @PathVariable Long id
    ) {

        decks.deleteByIdForCurrentTenant(id);
    }
}
