# Data Model & Schema Specification: Customer Product Activation Service

**Feature**: `001-product-activation`
**Date**: 2026-10-28
**Status**: Ready

## 1. Domain Entities & Value Objects

### 1.1 ActivationRequest (Aggregate Root)
Encapsulates activation state and domain transition invariants.

```
+-----------------------------------------------------------+
|                     ActivationRequest                     |
+-----------------------------------------------------------+
| - id: UUID                                                |
| - externalId: String (unique tracking identifier)         |
| - customerId: String                                      |
| - productCode: String                                     |
| - status: ActivationStatus                                |
| - failureReason: String (nullable)                        |
| - version: Long (optimistic concurrency control)          |
| - createdAt: Instant                                      |
| - updatedAt: Instant                                      |
+-----------------------------------------------------------+
| + create(externalId, customerId, productCode): Request    |
| + startProcessing(): void                                 |
| + complete(): void                                        |
| + fail(reason: String): void                              |
+-----------------------------------------------------------+
```

#### Status Enumeration (`ActivationStatus`)
- `PENDING`: Request received, persisted, and queued for provider dispatch.
- `PROCESSING`: Request dispatched to external provider, awaiting acknowledgement or undergoing active retry.
- `COMPLETED`: Activation confirmed successful by external provider (terminal state).
- `FAILED`: Activation rejected by provider (4xx) or max retries exceeded (terminal state).

#### Transition Rules:
- `PENDING` -> `PROCESSING`
- `PROCESSING` -> `COMPLETED`
- `PROCESSING` -> `FAILED`
- `PENDING` -> `FAILED` (if unrecoverable validation occurs before dispatch)
- Any transition from `COMPLETED` or `FAILED` throws `IllegalStateTransitionException`.

---

### 1.2 ActivationOutboxRecord (Transactional Outbox)
Guarantees reliable dispatch to external provider outside of active database transactions.

```
+-----------------------------------------------------------+
|                  ActivationOutboxRecord                   |
+-----------------------------------------------------------+
| - id: UUID                                                |
| - requestId: UUID                                         |
| - idempotencyKey: String                                  |
| - payload: String (JSON representation of request)         |
| - status: OutboxStatus (PENDING, SENT, FAILED)             |
| - retryCount: Integer                                     |
| - nextRetryAt: Instant                                    |
| - createdAt: Instant                                      |
| - updatedAt: Instant                                      |
+-----------------------------------------------------------+
| + markSent(): void                                        |
| + scheduleRetry(backoffSeconds: int): void                |
| + markFailed(): void                                      |
+-----------------------------------------------------------+
```

---

## 2. Relational Database Schema (PostgreSQL)

### Flyway Migration: `V1__init_activation_schema.sql`

```sql
CREATE TABLE activation_requests (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_activation_requests_external_id UNIQUE (external_id)
);

CREATE TABLE activation_outbox (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_outbox_request FOREIGN KEY (request_id) REFERENCES activation_requests(id) ON DELETE CASCADE
);

-- Indexes for performance and query optimization
CREATE INDEX idx_activation_requests_cust_status ON activation_requests (customer_id, status);
CREATE INDEX idx_activation_requests_created_at ON activation_requests (created_at DESC);
CREATE INDEX idx_outbox_status_next_retry ON activation_outbox (status, next_retry_at);
```

### Index Justification:
- `uk_activation_requests_external_id`: Unique B-Tree index strictly preventing duplicate insertions even under concurrent multi-threaded requests.
- `idx_activation_requests_cust_status`: Composite index optimizing paginated query filtering by customer and status (`GET /api/v1/requests?customerId=...&status=...`).
- `idx_outbox_status_next_retry`: Composite index allowing the outbox processor to poll due messages (`WHERE status = 'PENDING' AND next_retry_at <= NOW()`) efficiently without sequential table scans.
