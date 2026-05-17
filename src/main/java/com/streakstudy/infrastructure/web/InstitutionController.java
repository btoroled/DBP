package com.streakstudy.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.dto.InstitutionRequest;
import com.streakstudy.application.dto.InstitutionResponse;
import com.streakstudy.application.service.InstitutionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping("/{id}")
    public InstitutionResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
