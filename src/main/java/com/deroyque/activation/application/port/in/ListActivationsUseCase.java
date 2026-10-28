package com.deroyque.activation.application.port.in;

import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListActivationsUseCase {
    Page<ActivationRequest> list(String customerId, ActivationStatus status, Pageable pageable);
}
