package com.deroyque.activation.infrastructure.persistence.repository;

import com.deroyque.activation.infrastructure.persistence.entity.ActivationRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataActivationRepository
        extends JpaRepository<ActivationRequestEntity, UUID>, JpaSpecificationExecutor<ActivationRequestEntity> {

    Optional<ActivationRequestEntity> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
