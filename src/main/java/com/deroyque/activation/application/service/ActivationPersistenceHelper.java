package com.deroyque.activation.application.service;

import com.deroyque.activation.application.port.out.ActivationRepositoryPort;
import com.deroyque.activation.application.port.out.OutboxRepositoryPort;
import com.deroyque.activation.domain.model.ActivationRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ActivationPersistenceHelper {

    private final ActivationRepositoryPort activationRepository;
    private final OutboxRepositoryPort outboxRepository;

    public ActivationPersistenceHelper(ActivationRepositoryPort activationRepository,
                                       OutboxRepositoryPort outboxRepository) {
        this.activationRepository = activationRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ActivationRequest insertAtomically(ActivationRequest newRequest, String payload) {
        ActivationRequest saved = activationRepository.save(newRequest);
        outboxRepository.save(saved.getId(), UUID.randomUUID().toString(), payload);
        return saved;
    }
}
