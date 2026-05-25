package com.streakstudy.infrastructure.web;

import com.streakstudy.application.dto.BadgePurchaseRequest;
import com.streakstudy.application.service.StoreService;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping("/streak-freeze")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<Void> buyStreakFreeze(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        storeService.buyStreakFreeze(principal.userId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/badges")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<Void> buyBadge(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody BadgePurchaseRequest request) {

        storeService.buyBadge(principal.userId(), request.badgeName());
        return ResponseEntity.ok().build();
    }
}