package com.streakstudy.infrastructure.web.advice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.streakstudy.application.dto.CreateDeckRequest;
import com.streakstudy.application.dto.DeckResponse;
import com.streakstudy.application.service.DeckService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decks")
@Validated
public class DeckController {

    private final DeckService decks;

    public DeckController(DeckService decks) {
        this.decks = decks;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
    public DeckResponse getById(@PathVariable Long id) {
        return decks.getByIdForCurrentTenant(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        decks.deleteByIdForCurrentTenant(id);
    }
}