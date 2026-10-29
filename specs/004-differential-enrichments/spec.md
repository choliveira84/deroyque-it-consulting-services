# Feature Specification: Challenge Differential Enrichments

**Feature Branch**: `004-differential-enrichments`  
**Created**: 2026-10-28  
**Status**: Draft  
**Input**: User description: "cheque se há algum ponto diferencial que não foi implementado. sim, mas por uma nova specification"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Provider Throttling & HTTP 429 / 503 Retry-After Response Matrix (Priority: P1)

As a technical evaluator or backend engineer,  
I want the repository to document and specify the exact lifecycle behavior for all external provider responses, specifically including `HTTP 429 Too Many Requests` and `HTTP 503 Service Unavailable` with `Retry-After` headers,  
So that the service demonstrates robust distributed systems resilience, adhering to standard HTTP throttling semantics without overloading downstream partner systems.

**Why this priority**:
The challenge explicitly highlights the provider response behavior table with `HTTP 429 / 503 com Retry-After` as a key differentiator. Formalizing this behavior ensures complete compliance with evaluation rubrics.

**Independent Test**:
Inspect the technical documentation in `README.md`. The structured matrix defines:
- Provider response code (2xx, 400, 500, Timeout, 429/503 with `Retry-After`).
- Resulting status (`COMPLETED`, `FAILED`, `PROCESSING`).
- Retry decision (Yes/No, max attempts, backoff strategy).
- Technical justification explaining downstream rate limit protection and backoff jitter.

**Acceptance Scenarios**:
1. **Given** an external provider returning `HTTP 429` or `HTTP 503` with a `Retry-After` header, **When** processed by the system, **Then** the request remains in `PROCESSING` and schedules the next retry respecting the server-requested delay interval with randomized jitter.
2. **Given** an external provider returning `HTTP 400`, **When** received, **Then** the request transitions immediately to `FAILED` with no retry, recording the business error payload.
3. **Given** an external provider returning `HTTP 500` or timing out, **When** received, **Then** the request remains in `PROCESSING` up to 3 retries with exponential backoff and jitter before transitioning to `FAILED`.

---

### User Story 2 - Database Indexing Strategy and Justification for Paginated Queries (Priority: P1)

As a database administrator or technical evaluator reviewing query scalability,  
I want the `README.md` to formally justify the database indexes implemented in Flyway migrations,  
So that the rationale behind index composite ordering (`customer_id`, `status`), sorting (`created_at DESC`), and outbox polling is transparent and technically grounded.

**Why this priority**:
The challenge explicitly states: *"Listagem paginada com filtros. Considere quais índices seriam necessários e justifique no README."* While the indexes exist in SQL migrations, providing the engineering rationale completes the evaluation requirement.

**Independent Test**:
Inspect `README.md` under section 2.1. The documentation provides query execution analysis:
- Explains why `(customer_id, status)` orders high-cardinality `customer_id` first, followed by low-cardinality `status`.
- Explains why `idx_activation_requests_created_at` prevents in-memory *filesort* during descending paginated queries.
- Explains why the outbox composite index `(status, next_retry_at)` enables $O(\log N)$ polling by the background worker.

**Acceptance Scenarios**:
1. **Given** the paginated endpoint `GET /api/v1/requests?customerId=...&status=...`, **When** executed on large datasets, **Then** the database performs an Index Scan on `idx_activation_requests_cust_status` rather than a Sequential Scan.
2. **Given** sorting by creation date (`created_at DESC`), **When** executed, **Then** the query avoids disk/memory filesort operations.

---

### User Story 3 - API Hardening: Rate Limiting & BOLA Protection (Priority: P2)

As a security auditor or systems architect,  
I want the repository to document and configure boundary security measures (Request Body Size Limits, Rate Limiting / Throttling, and Broken Object Level Authorization protection),  
So that the API demonstrates enterprise-grade defense-in-depth as requested in the security differentials.

**Why this priority**:
Fulfills the security differentiator from item 7 of the challenge: *"Limite de tamanho do corpo da requisição e de taxa de chamadas. Isolamento entre consumidores (BOLA / OWASP API Top 10)."*

**Independent Test**:
Inspect `README.md` and application configuration to verify max payload size restrictions and clear architectural patterns for tenant isolation and rate limiting.

**Acceptance Scenarios**:
1. **Given** incoming API requests, **When** payload size exceeds configured limits, **Then** the request is rejected with `413 Payload Too Large`.
2. **Given** a client querying requests via `GET /api/v1/requests/{id}`, **When** evaluated against BOLA, **Then** the architecture enforces that callers can only access records matching their authenticated customer/tenant identity.

---

## Edge Cases

- **Missing `Retry-After` header on 429/503**: System defaults to exponential backoff with jitter ($2^n \times \text{base} + \text{jitter}$).
- **Non-integer `Retry-After` (HTTP Date vs Seconds)**: System supports seconds or falls back gracefully to default backoff.
- **Null filter values in paginated query**: When filters are omitted, queries rely on the primary key or `created_at` index to maintain sub-100ms response times.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST document a comprehensive provider response matrix in `README.md` with explicit rows for 2xx, 400, 500, Timeout, and 429/503 with `Retry-After`.
- **FR-002**: The provider response matrix MUST clearly state the resulting status (`COMPLETED`, `FAILED`, `PROCESSING`), whether retry is performed, and technical justification.
- **FR-003**: The system MUST document the database indexing strategy in `README.md`, justifying `idx_activation_requests_cust_status`, `idx_activation_requests_created_at`, and `idx_outbox_status_next_retry`.
- **FR-004**: The documentation MUST detail the column order rationale based on selectivity (cardinality) and index scan performance.
- **FR-005**: The system MUST document security boundary enforcement: payload size limits, Rate Limiting (Token Bucket via Gateway/Bucket4j), and BOLA prevention.
- **FR-006**: The application configuration (`application.yml`) MUST explicitly specify payload size constraints for production safety.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of optional differentials in the challenge document are either fully implemented in code or exhaustively justified in `README.md`.
- **SC-002**: Technical explanations provide concrete algorithms and mechanisms (B-Tree index ordering, exponential backoff with jitter, Token Bucket).
- **SC-003**: Existing automated test suite (28/28 tests) continues to pass cleanly with zero regressions.

---

## Assumptions

- Rate limiting in enterprise microservices is best positioned at the API Gateway / Ingress layer (AWS WAF, Kong, Spring Cloud Gateway) to drop abusive traffic before consuming application thread pool resources; this is documented conceptually while internal payload limits are configured in Spring Boot.
