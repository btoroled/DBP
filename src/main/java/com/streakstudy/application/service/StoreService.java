package com.streakstudy.application.service;

import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

@Service
public class StoreService {

    private final UserRepository userRepository;

    public StoreService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void buyStreakFreeze(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        User updatedUser = user.buyStreakFreeze();
        userRepository.save(updatedUser);
    }

    @Transactional
    public void buyBadge(Long userId, String badgeName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Badge badge;
        try {
            badge = Badge.valueOf(badgeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La insignia solicitada no existe en la tienda");
        }

        User updatedUser = user.buyBadge(badge);
        userRepository.save(updatedUser);
    }
}