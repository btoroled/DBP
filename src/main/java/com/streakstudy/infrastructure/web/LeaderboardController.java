package com.streakstudy.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.dto.LeaderboardUserResponse;
import com.streakstudy.application.service.LeaderboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Leaderboard")
@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Operation(summary = "Ranking de estudiantes por XP de la institucion del usuario autenticado")
    @GetMapping
    public List<LeaderboardUserResponse> getLeaderboard() {
        return leaderboardService.getLeaderboard();
    }
}
