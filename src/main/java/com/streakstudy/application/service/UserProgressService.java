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

        User updatedUser = user.addXp(1).incrementStreak(LocalDate.now());
        userRepository.save(updatedUser);
    }

    @Override
    @Transactional
    public UserProgressResponse execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));
        return new UserProgressResponse(user.xp(), user.currentStreak());
    }
}
