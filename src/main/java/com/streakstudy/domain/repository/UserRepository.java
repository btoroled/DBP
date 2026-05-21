package com.streakstudy.domain.repository;

import java.util.List;
import java.util.Optional;

import com.streakstudy.domain.model.User;

/**
 * Puerto de persistencia para User.
 *
 * <p>{@link #findByEmail(String)} es <b>cross-tenant</b> a proposito: el login
 * recibe un email y aun no sabe a que institucion pertenece el usuario.
 * El resto de operaciones son tenant-aware (filtran por institutionId).</p>
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndInstitutionId(Long id, Long institutionId);

    boolean existsByEmail(String email);

    // NUEVO MÉTODO PARA EL RANKING FILTRADO POR INSTITUCIÓN
    List<User> findLeaderboardByInstitutionId(Long institutionId);
}