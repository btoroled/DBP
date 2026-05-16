package com.streakstudy.application.service;

import org.springframework.stereotype.Service;

import com.streakstudy.application.dto.InstitutionRequest;
import com.streakstudy.application.dto.InstitutionResponse;
import com.streakstudy.domain.exception.DomainException;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.repository.InstitutionRepository;

/**
 * Servicio cross-tenant para gestionar instituciones.
 *
 * <p>Es invocado sin TenantContext: las operaciones de creacion y consulta
 * de instituciones son intencionadamente cross-tenant. En el futuro, los
 * controllers limitaran quien puede llamarlas (SUPER_ADMIN).</p>
 */
@Service
public class InstitutionService {

    private final InstitutionRepository institutions;

    public InstitutionService(InstitutionRepository institutions) {
        this.institutions = institutions;
    }

    public InstitutionResponse create(InstitutionRequest req) {
        String code = req.code().toLowerCase();
        if (institutions.existsByCode(code)) {
            throw new InstitutionCodeAlreadyExistsException(code);
        }
        Institution saved = institutions.save(Institution.newInstance(req.name(), code));
        return InstitutionResponse.from(saved);
    }

    public InstitutionResponse getById(Long id) {
        return institutions.findById(id)
            .map(InstitutionResponse::from)
            .orElseThrow(() -> new EntityNotFoundException("Institution", id));
    }

    public Institution getByIdOrThrow(Long id) {
        return institutions.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Institution", id));
    }

    public static class InstitutionCodeAlreadyExistsException extends DomainException {
        public InstitutionCodeAlreadyExistsException(String code) {
            super("Ya existe una institucion con code='%s'".formatted(code));
        }
    }
}
