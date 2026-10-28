package com.deroyque.activation.application.port.out;

import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ActivationRepositoryPort {

    ActivationRequest save(ActivationRequest request);

    Optional<ActivationRequest> findById(UUID id);

    Optional<ActivationRequest> findByExternalId(String externalId);

    Page<ActivationRequest> findAll(String customerId, ActivationStatus status, Pageable pageable);
}
