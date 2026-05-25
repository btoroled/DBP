package com.streakstudy.application.service;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streakstudy.application.dto.LeaderboardUserResponse;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;
@Service
public class LeaderboardService {
    private static final int DEFAULT_LEADERBOARD_SIZE = 10;
    private final UserRepository userRepository;
    public LeaderboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Transactional(readOnly = true)
    public List<LeaderboardUserResponse> getLeaderboard() {
        Long institutionId = TenantContext.requireInstitutionId();
        return userRepository.findTopStudentsByXp(institutionId, DEFAULT_LEADERBOARD_SIZE)
                .stream()
                .map(user -> new LeaderboardUserResponse(
                        user.id(),
                        user.fullName(),
                        user.streak(),
                        user.points()
                ))
                .collect(Collectors.toList());
    }
}
