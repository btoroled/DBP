package com.streakstudy.infrastructure.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streakstudy.application.dto.LeaderboardUserResponse;
import com.streakstudy.application.service.LeaderboardService;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardUserResponse>> getLeaderboard() {
        List<LeaderboardUserResponse> response = leaderboardService.getLeaderboard();
        return ResponseEntity.ok(response);
    }
}