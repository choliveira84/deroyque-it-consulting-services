package com.deroyque.activation.infrastructure.persistence.adapter;

import com.deroyque.activation.application.port.out.OutboxRepositoryPort;
import com.deroyque.activation.domain.model.OutboxMessage;
import com.deroyque.activation.domain.model.OutboxStatus;
import com.deroyque.activation.infrastructure.persistence.entity.ActivationOutboxEntity;
import com.deroyque.activation.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository repository;

    public OutboxRepositoryAdapter(SpringDataOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(UUID requestId, String idempotencyKey, String payload) {
        OutboxMessage domain = OutboxMessage.createPending(requestId, idempotencyKey, payload);
        repository.save(ActivationOutboxEntity.fromDomain(domain));
    }

    @Override
    public List<OutboxMessage> findPendingDue(Instant now) {
        return repository.findPendingDue(OutboxStatus.PENDING, now).stream()
                .map(ActivationOutboxEntity::toDomain)
                .toList();
    }

    @Override
    public void saveMessage(OutboxMessage message) {
        repository.save(ActivationOutboxEntity.fromDomain(message));
    }
}
