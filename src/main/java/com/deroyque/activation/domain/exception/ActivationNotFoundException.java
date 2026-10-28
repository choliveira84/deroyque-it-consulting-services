package com.deroyque.activation.domain.exception;

import java.util.UUID;

public class ActivationNotFoundException extends RuntimeException {
    public ActivationNotFoundException(UUID id) {
        super("Activation request not found with id: " + id);
    }

    public ActivationNotFoundException(String externalId) {
        super("Activation request not found with externalId: " + externalId);
    }
}
