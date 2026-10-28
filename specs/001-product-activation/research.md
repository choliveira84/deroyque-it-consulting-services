# Technical Research & Architecture Decisions: Customer Product Activation Service

**Feature**: `001-product-activation`
**Date**: 2026-10-28
**Status**: Completed

## 1. Concurrency, Idempotency & Deduplication Strategy

### Context & Problem
Origin systems (e.g., checkout engines, billing systems) retry HTTP calls upon network timeouts. Furthermore, multiple parallel web worker threads might send the identical `externalId` at the exact same millisecond.

A naive "check-then-insert" approach (`if (!repository.existsByExternalId(...)) { save(...); }`) suffers from the classic race condition (Time-Of-Check to Time-Of-Use - TOCTOU). Both threads will observe that the record does not exist, and both will attempt to insert.

### Architectural Decision
1. **Database-Level Invariant**: A unique constraint `uk_activation_requests_external_id` is created on `external_id` in PostgreSQL.
2. **Deterministic Concurrency Handling**:
   - The primary insert attempt is executed.
   - If a concurrent thread attempts insertion, PostgreSQL triggers a unique constraint violation, surfaced by Spring Data JPA as `DataIntegrityViolationException`.
   - The service catches this exception and performs a lookup by `externalId`.
3. **HTTP Semantics for Repetition**:
   - **Legitimate Retry (Identical Payload)**: If the existing record matches `customerId` and `productCode`, return `200 OK` with the existing activation representation (or `201 Created` if caught within the same transaction flow). This guarantees pure idempotent behavior.
   - **Conflicting Payload (Divergent productCode or customerId)**: If the `externalId` is already claimed by a different customer or product, return `409 Conflict` (RFC 9457 Problem Details) with clear error detail explaining the conflict.
4. **Optimistic Locking**:
   - The `ActivationRequest` entity includes a `@Version Long version` attribute.
   - Any concurrent state mutation (e.g., worker updating status while a query or user updates) uses optimistic locking, failing fast if stale data is updated.

---

## 2. Decoupling Relational Transactions from External HTTP Calls

### Context & Problem (Root Cause of Production Latency Scenario)
The challenge presents a production incident scenario:
- API latency degrades from 300 ms to 2.5 s.
- CPU is 45%, Memory is 60%.
- Database queries jump from 50 ms to 1.2 s.
- External provider latency jumps from 400 ms to 3.0 s.

**Root Cause**: In naive implementations, developers annotate the service method with `@Transactional` and call the external provider HTTP endpoint *inside* that method.
When the external provider degrades to 3.0 s, each thread holds an active relational database connection from the connection pool (HikariCP) for the full 3+ seconds while waiting on network I/O. As pool connections are exhausted, incoming requests (even fast read queries) block waiting for an idle connection, causing average DB latency to artificially rise to 1.2 s and overall API latency to balloon to 2.5 s, while CPU/Memory remain idle.

### Architectural Decision: Transactional Outbox Pattern
1. **Immediate Ingestion Transaction**:
   - Transaction 1 (Database only): Save `ActivationRequest` in `PENDING` status and insert an `ActivationOutboxRecord` in the same atomic database transaction.
   - Duration: < 10 ms.
   - Release database connection immediately.
   - Return `201 Created` with resource URL and initial status to client.
2. **Asynchronous Dispatch**:
   - An asynchronous worker (`@Async` / scheduled executor / Spring ApplicationEvent with `@TransactionalEventListener(phase = AFTER_COMMIT)`) picks up the outbox message.
   - Executes HTTP call to external provider outside of any database transaction.
3. **State Finalization Transaction**:
   - Upon receiving the external provider response, a separate, short database transaction updates `ActivationRequest` to `COMPLETED` or `FAILED` and marks the outbox record as `PROCESSED`.
   - Duration: < 5 ms.

---

## 3. Remote Provider Integration & Fault Tolerance

### Contract & Endpoints
- External Service: `POST /provider/v1/activate`
- Request: `{ "idempotencyKey": "UUID", "customerId": "...", "productCode": "..." }`

### Decision Matrix by Scenario

| Provider Response | New Activation Status | Retry? | Strategy & Justification |
| :--- | :--- | :--- | :--- |
| **Success (2xx)** | `COMPLETED` | No | Activation succeeded. Record updated to final success state. |
| **HTTP 400 (Bad Request)** | `FAILED` | No | Business/validation failure at provider. Automatic retry will never succeed and wastes resources. Record failure reason. |
| **HTTP 500 (Server Error)** | `PROCESSING` (during retry) / `FAILED` | Yes (up to 3 times) | Transient infrastructure error at provider. Retry with exponential backoff + jitter. If retries exhausted, mark `FAILED`. |
| **Timeout (Read/Connect)** | `PROCESSING` (during retry) / `FAILED` | Yes (up to 3 times) | Network timeout is ambiguous (provider might have activated). Retry passes original `idempotencyKey` so provider deduplicates. |
| **HTTP 429 / 503** | `PROCESSING` | Yes | Rate limiting or temporary downtime. Adhere to `Retry-After` header if present, else exponential backoff. |

### HTTP Client Configuration
- Use modern Spring 6 `RestClient` configured with an Apache HttpClient or standard `JdkClientHttpRequestFactory`.
- Explicit Timeouts:
  - Connect Timeout: 1,000 ms
  - Read Timeout: 3,000 ms
- Resilience: Idempotency Key header `X-Idempotency-Key` propagated on every attempt.

---

## 4. Error Handling Standard (RFC 9457)

Spring 6 / Spring Boot 3 provides native `ProblemDetail` support.

### Standard Response Structure:
```json
{
  "type": "https://api.deroyque.com/problems/validation-error",
  "title": "Invalid Request Data",
  "status": 400,
  "detail": "One or more validation constraints were violated.",
  "instance": "/api/v1/requests",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "invalidParams": [
    { "field": "productCode", "reason": "must not be blank" }
  ]
}
```

### Traceability:
- A filter injects/extracts `X-Correlation-Id` or generates a UUID, putting it into Logback's `MDC` (`traceId`).
- `@RestControllerAdvice` extracts the `traceId` from MDC and injects it into every `ProblemDetail` instance.
- No stack traces or internal SQL errors are ever serialized to clients.

---

## 5. Security Architecture (OAuth 2.0 Resource Server & JWT)

### Mechanism
- Spring Security 6 configured with `.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`.
- For production: Configured with `spring.security.oauth2.resourceserver.jwt.issuer-uri` or `jwk-set-uri`.
- For tests: Mocked using `SecurityMockMvcRequestPostProcessors.jwt()` or signed Nimbus test tokens.

### Scopes & Authorization
- `POST /api/v1/requests`: requires authority `SCOPE_activations:write`.
- `GET /api/v1/requests/**`: requires authority `SCOPE_activations:read`.
- BOLA prevention: When multi-tenant isolation is enforced, customer token subject/claims (`customerId`) must match the requested resource or possess administrative scope.

---

## 6. Testing Strategy & Pyramid

1. **Domain Unit Tests** (`src/test/java/.../domain/`):
   - Pure POJO tests for `ActivationRequest` state transitions (no Spring context, lightning fast).
2. **Repository & Concurrency Integration Tests** (`src/test/java/.../persistence/`):
   - Uses **Testcontainers PostgreSQL**.
   - Runs multithreaded ExecutorService with 10 concurrent threads inserting the same `externalId` simultaneously, asserting exactly 1 record is persisted.
3. **External Integration Tests** (`src/test/java/.../client/`):
   - Uses **WireMock** to simulate provider 200, 400, 500, and timeout delays.
4. **Web & Security Slice Tests** (`src/test/java/.../web/`):
   - `@WebMvcTest` with MockMvc verifying 401 Unauthorized (missing JWT), 403 Forbidden (missing scope), and RFC 9457 Problem Details formats.
5. **Architectural Guardrails** (`src/test/java/.../architecture/`):
   - **ArchUnit** tests validating that domain classes do not depend on Spring or web layers.
