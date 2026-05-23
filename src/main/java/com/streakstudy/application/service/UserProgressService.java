package com.streakstudy.application.service;
import com.streakstudy.application.dto.FinishReviewRequest;
import com.streakstudy.application.dto.UserProgressResponse;
import com.streakstudy.application.port.FinishReviewUseCase;
import com.streakstudy.application.port.GetUserProgressUseCase;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
@Service
public class UserProgressService implements FinishReviewUseCase, GetUserProgressUseCase {
    private final UserRepository userRepository;
    public UserProgressService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    @Transactional
    public void execute(Long userId, FinishReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));
        int xpEarned = calculateXpEarned(request);
        User updatedUser = user.addXp(xpEarned).incrementStreak(LocalDate.now());
        userRepository.save(updatedUser);
    }
    @Override
    @Transactional
    public UserProgressResponse execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));
        Set<String> badgeNames = user.badges().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return new UserProgressResponse(user.xp(), user.currentStreak(), user.streakFreezes(), badgeNames);
    }
    private int calculateXpEarned(FinishReviewRequest request) {
        int xpFromReviewedCards = request.reviewedCards();
        int xpFromStudyTime = request.durationMinutes() / 10;
        return xpFromReviewedCards + xpFromStudyTime;
    }
}
