package com.deroyque.activation.application.port.in;

import com.deroyque.activation.domain.model.ActivationRequest;

public interface CreateActivationUseCase {

    record CreateResult(ActivationRequest request, boolean isNew) {}

    CreateResult createActivation(String externalId, String customerId, String productCode);
}
