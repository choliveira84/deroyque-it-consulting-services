package com.deroyque.activation.infrastructure.persistence.adapter;

import com.deroyque.activation.application.port.out.ActivationRepositoryPort;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import com.deroyque.activation.infrastructure.persistence.entity.ActivationRequestEntity;
import com.deroyque.activation.infrastructure.persistence.repository.SpringDataActivationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ActivationRepositoryAdapter implements ActivationRepositoryPort {

    private final SpringDataActivationRepository repository;

    public ActivationRepositoryAdapter(SpringDataActivationRepository repository) {
        this.repository = repository;
    }

    @Override
    public ActivationRequest save(ActivationRequest request) {
        ActivationRequestEntity entity = repository.findById(request.getId())
                .map(existing -> {
                    existing.setStatus(request.getStatus());
                    existing.setFailureReason(request.getFailureReason());
                    existing.setUpdatedAt(request.getUpdatedAt());
                    return existing;
                })
                .orElseGet(() -> ActivationRequestEntity.fromDomain(request));
        ActivationRequestEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<ActivationRequest> findById(UUID id) {
        return repository.findById(id).map(ActivationRequestEntity::toDomain);
    }

    @Override
    public Optional<ActivationRequest> findByExternalId(String externalId) {
        return repository.findByExternalId(externalId).map(ActivationRequestEntity::toDomain);
    }

    @Override
    public Page<ActivationRequest> findAll(String customerId, ActivationStatus status, Pageable pageable) {
        Specification<ActivationRequestEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable).map(ActivationRequestEntity::toDomain);
    }
}
