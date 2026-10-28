package com.deroyque.activation.application.service;

import com.deroyque.activation.application.port.out.ActivationRepositoryPort;
import com.deroyque.activation.application.port.out.ExternalProviderPort;
import com.deroyque.activation.application.port.out.OutboxRepositoryPort;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.OutboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutboxProcessorService {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessorService.class);

    private final OutboxRepositoryPort outboxRepository;
    private final ActivationRepositoryPort activationRepository;
    private final ExternalProviderPort externalProvider;
    private final ObjectMapper objectMapper;
    private final int maxRetries;

    public OutboxProcessorService(OutboxRepositoryPort outboxRepository,
                                  ActivationRepositoryPort activationRepository,
                                  ExternalProviderPort externalProvider,
                                  ObjectMapper objectMapper,
                                  @Value("${activation.provider.max-retries:3}") int maxRetries) {
        this.outboxRepository = outboxRepository;
        this.activationRepository = activationRepository;
        this.externalProvider = externalProvider;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${activation.outbox.fixed-delay-ms:2000}", initialDelay = 1000)
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository.findPendingDue(Instant.now());
        if (pendingMessages.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox messages for processing", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            processSingleMessage(message);
        }
    }

    @Transactional
    public void processSingleMessage(OutboxMessage message) {
        ActivationRequest request = activationRepository.findById(message.getRequestId()).orElse(null);
        if (request == null) {
            log.warn("Outbox message references missing request id {}", message.getRequestId());
            message.markFailed();
            outboxRepository.saveMessage(message);
            return;
        }

        if (request.getStatus().isTerminal()) {
            message.markSent();
            outboxRepository.saveMessage(message);
            return;
        }

        request.startProcessing();
        request = activationRepository.save(request);

        Map<?, ?> payloadMap;
        try {
            payloadMap = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) {
            log.error("Failed to parse outbox payload for request {}", message.getRequestId(), e);
            request.fail("Malformed outbox payload");
            activationRepository.save(request);
            message.markFailed();
            outboxRepository.saveMessage(message);
            return;
        }

        String customerId = String.valueOf(payloadMap.get("customerId"));
        String productCode = String.valueOf(payloadMap.get("productCode"));

        // Remote call executed outside active long transaction
        ExternalProviderPort.ActivationResult result = externalProvider.activate(
                message.getIdempotencyKey(),
                customerId,
                productCode
        );

        handleActivationResult(result, request, message);
    }

    private void handleActivationResult(ExternalProviderPort.ActivationResult result,
                                        ActivationRequest request,
                                        OutboxMessage message) {
        switch (result) {
            case ExternalProviderPort.ActivationResult.Success success -> {
                log.info("Request {} successfully completed by external provider with code {}",
                        request.getId(), success.activationCode());
                request.complete();
                message.markSent();
            }
            case ExternalProviderPort.ActivationResult.ClientError clientError -> {
                log.warn("Request {} rejected by provider (4xx): {}", request.getId(), clientError.message());
                request.fail("Provider rejected activation: " + clientError.message());
                message.markFailed();
            }
            case ExternalProviderPort.ActivationResult.ServerError serverError -> {
                handleRetryableError(request, message, "Provider server error: " + serverError.message());
            }
            case ExternalProviderPort.ActivationResult.Timeout timeout -> {
                handleRetryableError(request, message, "Provider timeout: " + timeout.message());
            }
        }

        activationRepository.save(request);
        outboxRepository.saveMessage(message);
    }

    private void handleRetryableError(ActivationRequest request, OutboxMessage message, String errorReason) {
        if (message.getRetryCount() < maxRetries) {
            long backoffSeconds = (long) Math.pow(2, message.getRetryCount() + 1);
            log.info("Scheduling retry #{} in {} seconds for request {}",
                    message.getRetryCount() + 1, backoffSeconds, request.getId());
            message.scheduleRetry(backoffSeconds);
        } else {
            log.error("Max retries ({}) exhausted for request {}. Marking as FAILED.", maxRetries, request.getId());
            request.fail(errorReason + " (Max retries exhausted)");
            message.markFailed();
        }
    }
}
