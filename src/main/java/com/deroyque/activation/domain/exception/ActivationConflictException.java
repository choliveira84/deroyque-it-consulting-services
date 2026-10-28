package com.deroyque.activation.domain.exception;

public class ActivationConflictException extends RuntimeException {
    public ActivationConflictException(String externalId, String message) {
        super("Conflict on activation with externalId '" + externalId + "': " + message);
    }
}
