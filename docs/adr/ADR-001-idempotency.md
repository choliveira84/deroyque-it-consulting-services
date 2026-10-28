# ADR-001: Deduplication & Idempotency Strategy

## Status
Accepted

## Context
Origin systems (e.g. checkouts, ERPs, event streams) often retry activation requests or send requests concurrently across distributed instances. We need to prevent duplicate product activations for the same origin identifier (`externalId`).

A naive "check-if-exists-then-insert" in application logic is vulnerable to race conditions (TOCTOU). When two identical requests arrive simultaneously, both queries return false, and both attempt to insert.

## Decision
1. **Schema-Level Enforcement**: Declare a unique constraint (`uk_activation_requests_external_id`) on column `external_id` in PostgreSQL.
2. **Handling Concurrent Inserts**: Catch `DataIntegrityViolationException` when a unique constraint violation occurs during concurrent execution.
3. **HTTP Semantics for Repetition**:
   - **Legitimate Retry (Identical Payload)**: When a request arrives with an already-persisted `externalId` and matching `customerId` and `productCode`, the service returns `200 OK` (or `201 Created` on the initial thread) with the existing representation.
   - **Payload Divergence (Conflicting Data)**: If a request presents an `externalId` already bound to a different `productCode` or `customerId`, the service returns `409 Conflict` (RFC 9457 Problem Details) rejecting the operation.
4. **Optimistic Locking**: Use JPA `@Version` on the activation request entity to prevent lost updates during concurrent processing.

## Consequences
- Guaranteed zero duplicate activations regardless of concurrency.
- High performance: no distributed lock manager (Redis/Redlock) needed for basic idempotency.
