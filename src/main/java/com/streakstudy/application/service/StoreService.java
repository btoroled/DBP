package com.streakstudy.application.service;

import com.streakstudy.application.event.BadgeEarnedEvent;
import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.streakstudy.domain.exception.EntityNotFoundException;

@Service
public class StoreService {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StoreService(UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void buyStreakFreeze(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        User updatedUser = user.buyStreakFreeze();
        userRepository.save(updatedUser);
    }

    @Transactional
    public void buyBadge(Long userId, Badge badge) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        User updatedUser = user.buyBadge(badge);
        userRepository.save(updatedUser);

        eventPublisher.publishEvent(new BadgeEarnedEvent(
                updatedUser.id(),
                updatedUser.institutionId(),
                updatedUser.email(),
                updatedUser.fullName(),
                badge.name(),
                badge.displayName(),
                badge.description()));
    }
}
