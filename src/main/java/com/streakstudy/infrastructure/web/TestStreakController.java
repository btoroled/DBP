package com.streakstudy.infrastructure.web;

import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.service.StreakResetService;

@RestController
@RequestMapping("/api/v1/test")
@Profile("dev")
@PreAuthorize("hasAuthority('STUDENT')")
public class TestStreakController {

    private final StreakResetService streakResetService;

    public TestStreakController(StreakResetService streakResetService) {
        this.streakResetService = streakResetService;
    }

    @GetMapping("/trigger-cron")
    public ResponseEntity<Void> triggerCron(Authentication authentication) {
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
        streakResetService.executeReset(principal.institutionId());

        return ResponseEntity.ok().build();
    }
}