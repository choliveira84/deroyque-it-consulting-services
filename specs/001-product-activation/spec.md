# Feature Specification: Customer Product Activation Service

**Feature Branch**: `001-product-activation`

**Created**: 2026-10-28

**Status**: Ready for Planning

**Input**: User description: "Serviço de ativação de produtos de clientes: registro de solicitações, consulta de status, tratamento de duplicidade e concorrência, e integração com provedor externo."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Product Activation Request (Priority: P1)

As a client system (e.g., e-commerce, CRM, or billing portal), I want to submit a product activation request for a customer so that the requested product can be registered, tracked, and activated.

**Why this priority**: Core value of the service. Without request ingestion and tracking, no activations can take place.

**Independent Test**: Can be fully tested by submitting a valid activation payload via `POST /api/v1/requests` and receiving a `201 Created` status with the persisted request representation and `Location` header.

**Acceptance Scenarios**:

1. **Given** valid input with `externalId` "EXT-1001", `customerId` "CUST-001", and `productCode` "PROD-FIBER", **When** client sends `POST /api/v1/requests`, **Then** system creates the activation request with initial status `PENDING`, generates a unique identifier, records UTC timestamps, and responds with `201 Created` and a `Location` header pointing to the created resource.
2. **Given** request payload with missing or blank mandatory fields (`customerId`, `productCode`, or `externalId`), **When** client sends `POST /api/v1/requests`, **Then** system rejects the request with `400 Bad Request` and returns an RFC 9457 Problem Details payload detailing each validation failure.

---

### User Story 2 - Idempotent Handling of Duplicate & Concurrent Submissions (Priority: P1)

As an origin client system subject to network retries or parallel event dispatch, I want the activation service to handle repeated or concurrent requests with the same `externalId` safely so that duplicate activations and state corruption never occur.

**Why this priority**: Mission-critical distributed systems requirement. Prevents double-charging, double-provisioning, and race-condition data anomalies.

**Independent Test**: Can be fully tested by submitting identical and conflicting requests with identical `externalId` sequentially and concurrently across multiple threads against the database.

**Acceptance Scenarios**:

1. **Given** an existing activation request with `externalId` "EXT-1001" and payload for customer "CUST-001" and product "PROD-FIBER", **When** a client resubmits an identical request (same `externalId`, `customerId`, and `productCode`), **Then** system recognizes a legitimate idempotent retry, returns `200 OK` with the existing request data, and creates no new record.
2. **Given** an existing activation request with `externalId` "EXT-1001" and product "PROD-FIBER", **When** a client submits a new request with identical `externalId` "EXT-1001" but divergent payload (e.g., `productCode` "PROD-MOBILE"), **Then** system detects a semantic collision and returns `409 Conflict` (or `422 Unprocessable Entity`) indicating payload divergence on an existing `externalId`.
3. **Given** two identical requests with the same unused `externalId` arriving simultaneously across concurrent threads, **When** both are processed in parallel, **Then** database uniqueness guarantees exactly one persistent record is created; one request receives `201 Created` and the concurrent request resolves idempotently without error or data duplication.

---

### User Story 3 - Query Activation Status & History (Priority: P2)

As a client system or support operator, I want to query the status of a specific activation request or list requests with pagination and filters so that activation progress can be monitored and audited.

**Why this priority**: Required for operational visibility and consumer status polling.

**Independent Test**: Can be tested by querying `/api/v1/requests/{id}` for existing and non-existing IDs, and by executing filtered queries against `/api/v1/requests`.

**Acceptance Scenarios**:

1. **Given** an existing activation request with ID "0b7f3c1e-5d2a-4c4e-9a51-2f8d7b1e6c90", **When** client sends `GET /api/v1/requests/0b7f3c1e-5d2a-4c4e-9a51-2f8d7b1e6c90`, **Then** system responds with `200 OK` and the complete state representation (`id`, `externalId`, `customerId`, `productCode`, `status`, `createdAt`, `updatedAt`).
2. **Given** a request ID that does not exist in the system, **When** client queries `GET /api/v1/requests/{nonExistentId}`, **Then** system responds with `404 Not Found` in RFC 9457 format.
3. **Given** a set of activation records with various statuses and customer IDs, **When** client sends `GET /api/v1/requests?status=PENDING&customerId=CUST-001&page=0&size=10`, **Then** system returns a paginated list matching the criteria along with pagination metadata (`totalElements`, `totalPages`, `page`, `size`).

---

### User Story 4 - Resilient External Provider Activation (Priority: P2)

As the product activation service, I want to communicate reliably with the external activation provider without tying up database transactions so that downstream latency or failure does not exhaust service resources.

**Why this priority**: Protects service availability and ensures reliable eventual consistency across distributed boundaries.

**Independent Test**: Can be tested by invoking the activation flow against mock provider scenarios (2xx success, 400 bad request, 500 internal error, and read timeouts) and asserting status progression and retry behavior.

**Acceptance Scenarios**:

1. **Given** an activation request in `PENDING` status, **When** the integration client calls the external provider with an idempotency key and receives `200 OK`, **Then** request transitions to `PROCESSING` and subsequently to `COMPLETED`.
2. **Given** an activation request in `PENDING` status, **When** external provider returns `400 Bad Request` (business rejection), **Then** request transitions to `FAILED` with failure details recorded, and no automatic retry is attempted.
3. **Given** an activation request in `PENDING` status, **When** external provider times out or returns `500 Internal Server Error`, **Then** system triggers configured retry policy with exponential backoff and jitter, preserving idempotency key; if retries exceed maximum threshold, status transitions to `FAILED`.
4. **Given** a request currently in terminal state `COMPLETED` or `FAILED`, **When** any internal or external trigger attempts transition back to `PROCESSING` or `PENDING`, **Then** domain rejects the invalid transition with domain exception.

---

### User Story 5 - Enterprise Security & Observable Error Handling (Priority: P3)

As a security auditor and platform engineer, I want the API protected by OAuth 2.0 / JWT scopes and errors represented in RFC 9457 with correlation IDs so that unauthorized access is blocked and production diagnostics are streamlined.

**Why this priority**: Required for enterprise multi-tenant integration, auditability, and production diagnostics.

**Independent Test**: Can be tested via slice tests verifying that requests without tokens receive 401, requests with read-only tokens calling POST receive 403, and all error responses contain `traceId` with zero stack traces.

**Acceptance Scenarios**:

1. **Given** an unauthenticated client request, **When** accessing any endpoint, **Then** system responds with `401 Unauthorized`.
2. **Given** a client with token bearing only `SCOPE_activations:read`, **When** attempting `POST /api/v1/requests`, **Then** system responds with `403 Forbidden`.
3. **Given** any runtime or validation error, **When** response is returned, **Then** body contains `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`, and `traceId`, with no Java exception stack traces exposed.

---

### Edge Cases

- **Slow External Provider (> 3 seconds)**: Database connection must not be held while waiting for HTTP response; request status remains safely tracked.
- **Microsecond Concurrent Duplicates**: Two threads attempt to insert the same `externalId` simultaneously; database unique constraint intercepts collision and application resolves the loser gracefully to idempotent response.
- **Provider Timeout Ambiguity**: External provider receives request, processes activation, but network response drops; retry passes original idempotency key to prevent double activation at provider.
- **Entity State Stale Update**: Two concurrent processes attempt to modify the same activation request; optimistic locking detects version mismatch and rejects stale update.
- **Malformed JSON or Excessively Large Payload**: Request rejected at boundary with 400 Bad Request before hitting domain logic.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose an endpoint `POST /api/v1/requests` to create activation requests requiring non-blank `externalId`, `customerId`, and `productCode`.
- **FR-002**: System MUST assign a UUID `id`, initial status `PENDING`, and UTC `createdAt`/`updatedAt` timestamps in ISO-8601 format upon creation.
- **FR-003**: System MUST enforce a unique constraint on `externalId` in the persistence layer.
- **FR-004**: System MUST handle duplicate `externalId` submissions idempotently: returning `200 OK` with existing record if payload matches, or `409 Conflict` if payload diverges.
- **FR-005**: System MUST expose an endpoint `GET /api/v1/requests/{id}` returning `200 OK` with the activation record, or `404 Not Found` if the identifier is unknown.
- **FR-006**: System MUST expose an endpoint `GET /api/v1/requests` supporting pagination (`page`, `size`, `sort`) and optional filtering by `status` and `customerId`.
- **FR-007**: System MUST enforce domain-controlled state transitions: `PENDING` -> `PROCESSING` -> `COMPLETED` or `FAILED`, strictly prohibiting transitions out of terminal states.
- **FR-008**: System MUST decouple relational database transactions from external HTTP integration calls (`POST /provider/v1/activate`) using asynchronous execution / Transactional Outbox pattern.
- **FR-009**: System MUST transmit a unique idempotency key to the external provider on all activation attempts.
- **FR-010**: System MUST configure explicit connection and read timeouts on external HTTP client calls.
- **FR-011**: System MUST categorize external provider responses: 2xx -> COMPLETED; 4xx -> FAILED (no retry); 5xx / Timeout -> Retry with backoff, then FAILED when max retries exceeded.
- **FR-012**: System MUST format all API errors according to RFC 9457 Problem Details (`application/problem+json`), including correlation `traceId`.
- **FR-013**: System MUST protect endpoints using OAuth 2.0 Resource Server with JWT token validation, segregating `activations:read` and `activations:write` scopes.
- **FR-014**: System MUST employ optimistic locking (`@Version`) on the activation aggregate to prevent lost updates under concurrent modifications.

### Key Entities

- **ActivationRequest**:
  - Represents the core business request for product activation.
  - Attributes: `id` (UUID), `externalId` (String, unique, indexed), `customerId` (String, indexed), `productCode` (String), `status` (Enum: PENDING, PROCESSING, COMPLETED, FAILED), `failureReason` (String, nullable), `version` (Long, concurrency control), `createdAt` (Instant), `updatedAt` (Instant).
- **ActivationOutboxRecord / ActivationEvent**:
  - Represents the decoupled, transactional task scheduled for remote provider execution.
  - Attributes: `id` (UUID), `requestId` (UUID), `idempotencyKey` (String), `payload` (String/JSON), `status` (Enum: PENDING, SENT, FAILED), `retryCount` (Integer), `lastAttemptAt` (Instant), `createdAt` (Instant).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of concurrent or duplicate requests with identical `externalId` result in exactly one persistent record in the database.
- **SC-002**: 0 database connection pool lockups or thread starvation incidents during external provider latency spikes or timeouts.
- **SC-003**: Ingestion latency (P95) for `POST /api/v1/requests` remains under 100ms when external provider is simulated with 3000ms latency.
- **SC-004**: 100% of error responses comply with RFC 9457 with zero internal stack traces or database schema identifiers leaked.
- **SC-005**: 100% of invalid status transitions (e.g., `COMPLETED` -> `PROCESSING`) are rejected by domain invariants.
- **SC-006**: Complete test suite (unit, concurrency integration with Testcontainers, WireMock integrations, and ArchUnit architectural rules) passes in a single automated command (`./mvnw verify`).

## Assumptions

- Origin client systems are capable of providing a stable `externalId` for each unique business intent.
- External activation provider supports idempotent execution when supplied with an idempotency key header or token.
- JWT tokens are issued by a trusted OAuth 2.0 Authorization Server; the application validates tokens locally using public keys / JWKS.
- Asynchronous dispatch / Transactional Outbox provides eventual consistency for activation status completion without sacrificing initial API response time.
