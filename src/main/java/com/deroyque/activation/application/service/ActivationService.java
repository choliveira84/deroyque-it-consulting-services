package com.deroyque.activation.application.service;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.port.in.GetActivationUseCase;
import com.deroyque.activation.application.port.in.ListActivationsUseCase;
import com.deroyque.activation.application.port.out.ActivationRepositoryPort;
import com.deroyque.activation.domain.exception.ActivationConflictException;
import com.deroyque.activation.domain.exception.ActivationNotFoundException;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActivationService implements CreateActivationUseCase, GetActivationUseCase, ListActivationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivationService.class);

    private final ActivationRepositoryPort activationRepository;
    private final ActivationPersistenceHelper persistenceHelper;
    private final ObjectMapper objectMapper;

    public ActivationService(ActivationRepositoryPort activationRepository,
                             ActivationPersistenceHelper persistenceHelper,
                             ObjectMapper objectMapper) {
        this.activationRepository = activationRepository;
        this.persistenceHelper = persistenceHelper;
        this.objectMapper = objectMapper;
    }

    @Override
    public CreateResult createActivation(String externalId, String customerId, String productCode) {
        // 1. Fast check for duplicate externalId
        Optional<ActivationRequest> existing = activationRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            return handleExisting(existing.get(), customerId, productCode);
        }

        // 2. Create new domain request
        ActivationRequest newRequest = ActivationRequest.create(externalId, customerId, productCode);
        String payload = serializePayload(newRequest);

        try {
            // Save request and outbox atomically in an isolated transaction
            ActivationRequest saved = persistenceHelper.insertAtomically(newRequest, payload);
            log.info("Activation request created successfully with id {} for externalId {}", saved.getId(), externalId);
            return new CreateResult(saved, true);

        } catch (DataIntegrityViolationException ex) {
            // Concurrency race condition: Another thread inserted externalId concurrently
            log.warn("Concurrent duplicate insertion intercepted for externalId {}. Resolving idempotently.", externalId);
            ActivationRequest concurrent = activationRepository.findByExternalId(externalId)
                    .orElseThrow(() -> ex);
            return handleExisting(concurrent, customerId, productCode);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActivationRequest getById(UUID id) {
        return activationRepository.findById(id)
                .orElseThrow(() -> new ActivationNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivationRequest> list(String customerId, ActivationStatus status, Pageable pageable) {
        return activationRepository.findAll(customerId, status, pageable);
    }

    private CreateResult handleExisting(ActivationRequest existing, String customerId, String productCode) {
        if (existing.matchesPayload(customerId, productCode)) {
            log.info("Idempotent replay detected for externalId {}. Returning existing record with status {}",
                    existing.getExternalId(), existing.getStatus());
            return new CreateResult(existing, false);
        } else {
            log.warn("Conflict detected for externalId {}: Divergent customer or product code", existing.getExternalId());
            throw new ActivationConflictException(
                    existing.getExternalId(),
                    "An activation request with this externalId already exists with divergent productCode or customerId"
            );
        }
    }

    private String serializePayload(ActivationRequest request) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "requestId", request.getId().toString(),
                    "customerId", request.getCustomerId(),
                    "productCode", request.getProductCode()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
