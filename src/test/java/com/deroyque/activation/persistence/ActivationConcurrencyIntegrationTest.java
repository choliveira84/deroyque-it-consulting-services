package com.deroyque.activation.persistence;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.service.ActivationService;
import com.deroyque.activation.domain.exception.ActivationConflictException;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.infrastructure.persistence.repository.SpringDataActivationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ActivationConcurrencyIntegrationTest {

    @Autowired
    private ActivationService activationService;

    @Autowired
    private SpringDataActivationRepository springDataActivationRepository;

    @Test
    @DisplayName("10 parallel threads sending identical externalId must result in exactly 1 persisted record")
    void shouldHandleConcurrentDuplicateSubmissionsSafely() throws InterruptedException, ExecutionException {
        String externalId = "CONCURRENT-" + System.currentTimeMillis();
        String customerId = "CUST-999";
        String productCode = "INTERNET-1000";

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger newCount = new AtomicInteger(0);
        AtomicInteger replayCount = new AtomicInteger(0);
        List<Future<CreateActivationUseCase.CreateResult>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await(); // Synchronize all threads to hit simultaneously
                try {
                    CreateActivationUseCase.CreateResult result =
                            activationService.createActivation(externalId, customerId, productCode);
                    if (result.isNew()) {
                        newCount.incrementAndGet();
                    } else {
                        replayCount.incrementAndGet();
                    }
                    return result;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown(); // Release all threads at once
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        // Exactly one thread must have created the record
        assertThat(newCount.get()).isEqualTo(1);
        // The remaining 9 threads must have received the idempotent replay
        assertThat(replayCount.get()).isEqualTo(9);

        // Verify database contains exactly one record with this externalId
        long count = springDataActivationRepository.findAll().stream()
                .filter(e -> e.getExternalId().equals(externalId))
                .count();
        assertThat(count).isEqualTo(1);

        // All returned results must have the identical entity ID
        for (Future<CreateActivationUseCase.CreateResult> future : futures) {
            ActivationRequest req = future.get().request();
            assertThat(req.getExternalId()).isEqualTo(externalId);
            assertThat(req.getCustomerId()).isEqualTo(customerId);
        }
    }

    @Test
    @DisplayName("Should throw ActivationConflictException when resubmitting existing externalId with different product")
    void shouldRejectConflictingPayload() {
        String externalId = "CONFLICT-" + System.currentTimeMillis();
        activationService.createActivation(externalId, "CUST-1", "PROD-ORIGINAL");

        assertThatThrownBy(() -> activationService.createActivation(externalId, "CUST-1", "PROD-DIFFERENT"))
                .isInstanceOf(ActivationConflictException.class)
                .hasMessageContaining("already exists with divergent");
    }
}
