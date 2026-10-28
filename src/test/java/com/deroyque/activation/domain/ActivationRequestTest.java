package com.deroyque.activation.domain;

import com.deroyque.activation.domain.exception.InvalidStateTransitionException;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationRequestTest {

    @Test
    @DisplayName("Should create activation request with PENDING status and valid attributes")
    void shouldCreateActivationRequestSuccessfully() {
        ActivationRequest request = ActivationRequest.create("EXT-101", "CUST-001", "PROD-FIBER");

        assertThat(request.getId()).isNotNull();
        assertThat(request.getExternalId()).isEqualTo("EXT-101");
        assertThat(request.getCustomerId()).isEqualTo("CUST-001");
        assertThat(request.getProductCode()).isEqualTo("PROD-FIBER");
        assertThat(request.getStatus()).isEqualTo(ActivationStatus.PENDING);
        assertThat(request.getFailureReason()).isNull();
        assertThat(request.getVersion()).isEqualTo(0L);
        assertThat(request.getCreatedAt()).isNotNull();
        assertThat(request.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating with blank fields")
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> ActivationRequest.create(" ", "CUST-001", "PROD-FIBER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalId");

        assertThatThrownBy(() -> ActivationRequest.create("EXT-101", " ", "PROD-FIBER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId");

        assertThatThrownBy(() -> ActivationRequest.create("EXT-101", "CUST-001", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productCode");
    }

    @Test
    @DisplayName("Should transition from PENDING to PROCESSING to COMPLETED")
    void shouldTransitionToCompletedSuccessfully() {
        ActivationRequest request = ActivationRequest.create("EXT-101", "CUST-001", "PROD-FIBER");

        request.startProcessing();
        assertThat(request.getStatus()).isEqualTo(ActivationStatus.PROCESSING);

        request.complete();
        assertThat(request.getStatus()).isEqualTo(ActivationStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should transition to FAILED with failure reason")
    void shouldTransitionToFailedSuccessfully() {
        ActivationRequest request = ActivationRequest.create("EXT-101", "CUST-001", "PROD-FIBER");
        request.startProcessing();

        request.fail("Provider rejected product code");

        assertThat(request.getStatus()).isEqualTo(ActivationStatus.FAILED);
        assertThat(request.getFailureReason()).isEqualTo("Provider rejected product code");
    }

    @Test
    @DisplayName("Should reject invalid state transition once COMPLETED")
    void shouldRejectTransitionFromCompleted() {
        ActivationRequest request = ActivationRequest.create("EXT-101", "CUST-001", "PROD-FIBER");
        request.startProcessing();
        request.complete();

        assertThatThrownBy(request::startProcessing)
                .isInstanceOf(InvalidStateTransitionException.class);

        assertThatThrownBy(() -> request.fail("Late error"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Should check payload equality for idempotency matching")
    void shouldMatchPayloadCorrectly() {
        ActivationRequest request = ActivationRequest.create("EXT-101", "CUST-001", "PROD-FIBER");

        assertThat(request.matchesPayload("CUST-001", "PROD-FIBER")).isTrue();
        assertThat(request.matchesPayload("CUST-002", "PROD-FIBER")).isFalse();
        assertThat(request.matchesPayload("CUST-001", "PROD-OTHER")).isFalse();
    }
}
