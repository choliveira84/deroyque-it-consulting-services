package com.deroyque.activation.application.port.out;

import com.deroyque.activation.domain.model.OutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepositoryPort {

    void save(UUID requestId, String idempotencyKey, String payload);

    List<OutboxMessage> findPendingDue(Instant now);

    void saveMessage(OutboxMessage message);
}
