package com.streakstudy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.dto.FinishReviewRequest;
import com.streakstudy.application.dto.UserProgressResponse;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.Badge;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;
import com.streakstudy.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserProgressServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserProgressService service;

    @Test
    void execute_sumaXpSegunTarjetasYTiempoEIncrementaRacha() {
        User user = new User(10L, 1L, "alice@test.com", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 10, 2, LocalDate.now().minusDays(1), 1, Set.of());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.execute(10L, new FinishReviewRequest(12, 20));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.xp()).isEqualTo(24);
        assertThat(saved.currentStreak()).isEqualTo(3);
    }

    @Test
    void execute_lanzaCuandoElUsuarioNoExiste() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L, new FinishReviewRequest(5, 10)))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void execute_obtenerProgreso_mapeaCamposYBadges() {
        User user = new User(10L, 1L, "alice@test.com", "HASH", "Alice", UserRole.STUDENT,
            Instant.now(), 15, 4, LocalDate.now(), 2, Set.of(Badge.STREAK_STARTER));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserProgressResponse response = service.execute(10L);

        assertThat(response.xp()).isEqualTo(15);
        assertThat(response.currentStreak()).isEqualTo(4);
        assertThat(response.streakFreezes()).isEqualTo(2);
        assertThat(response.badges()).containsExactly("STREAK_STARTER");
    }
}
