package com.deroyque.activation.application.port.in;

import com.deroyque.activation.domain.model.ActivationRequest;

import java.util.UUID;

public interface GetActivationUseCase {
    ActivationRequest getById(UUID id);
}
