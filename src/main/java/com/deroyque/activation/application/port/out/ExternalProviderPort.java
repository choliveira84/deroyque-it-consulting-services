package com.deroyque.activation.application.port.out;

public interface ExternalProviderPort {

    sealed interface ActivationResult {
        record Success(String activationCode) implements ActivationResult {}
        record ClientError(int statusCode, String message) implements ActivationResult {}
        record ServerError(int statusCode, String message) implements ActivationResult {}
        record Timeout(String message) implements ActivationResult {}
    }

    ActivationResult activate(String idempotencyKey, String customerId, String productCode);
}
