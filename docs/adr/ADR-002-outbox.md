# ADR-002: Transactional Outbox Pattern for Remote Integration

## Status
Accepted

## Context
When an activation request is ingested, the system must trigger external activation via `POST /provider/v1/activate`.
Executing HTTP calls directly inside a database transaction causes critical vulnerabilities:
1. Long-running remote calls hold active database connections open, leading to HikariCP connection pool exhaustion and cascading latency.
2. If the application crashes after the remote call succeeds but before database commit, state is lost while external activation already took place.

## Decision
Adopt the **Transactional Outbox Pattern**:
1. When `POST /api/v1/requests` is called, persist the `ActivationRequest` entity (in `PENDING` status) and an `ActivationOutboxRecord` in the **same atomic database transaction**.
2. Transaction duration is under 10 ms. The database connection is released immediately and `201 Created` is returned to the client.
3. An asynchronous worker (`OutboxProcessorService`) reads pending outbox messages and executes the remote HTTP call completely outside any relational database transaction.
4. The remote call propagates an idempotency header `X-Idempotency-Key` and uses explicit timeouts (1s connect, 3s read).
5. A separate, short transaction updates the activation status (`COMPLETED` or `FAILED`) and marks the outbox message `SENT` or schedules a retry with exponential backoff.

## Consequences
- Total protection of database connection pool from downstream provider latency.
- Guaranteed eventual consistency.
