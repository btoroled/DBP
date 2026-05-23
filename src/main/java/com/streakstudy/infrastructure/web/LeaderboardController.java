package com.streakstudy.infrastructure.web;

import com.streakstudy.application.dto.LeaderBoardEntryDTo;
import com.streakstudy.application.service.LeaderBoardService;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@PreAuthorize("hasAuthority('STUDENT')") // Protegido para que solo entren alumnos autenticados
public class LeaderBoardController {

    private final LeaderBoardService leaderboardService;

    public LeaderBoardController(LeaderBoardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderBoardEntryDTo>> getInstitutionLeaderboard(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int top) { // Por defecto trae el Top 10, pero se puede cambiar

        // Extraemos de forma segura el principal para obtener el Tenant
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();

        // Ejecutamos la lógica del ranking filtrada por su universidad
        List<LeaderBoardEntryDTo> ranking = leaderboardService.getLeaderboard(principal.institutionId(), top);

        return ResponseEntity.ok(ranking);
    }
}