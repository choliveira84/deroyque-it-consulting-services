package com.deroyque.activation.infrastructure.persistence.repository;

import com.deroyque.activation.domain.model.OutboxStatus;
import com.deroyque.activation.infrastructure.persistence.entity.ActivationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<ActivationOutboxEntity, UUID> {

    @Query("SELECT o FROM ActivationOutboxEntity o WHERE o.status = :status AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) ORDER BY o.createdAt ASC")
    List<ActivationOutboxEntity> findPendingDue(@Param("status") OutboxStatus status, @Param("now") Instant now);
}
