package com.deroyque.activation.domain.exception;

import com.deroyque.activation.domain.model.ActivationStatus;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(ActivationStatus current, ActivationStatus target) {
        super("Cannot transition activation from " + current + " to " + target);
    }
}
