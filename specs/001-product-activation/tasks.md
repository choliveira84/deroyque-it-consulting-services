---
description: "Task list for Customer Product Activation Service implementation"
---

# Tasks: Customer Product Activation Service

**Input**: Design documents from `/specs/001-product-activation/` (`plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `.specify/memory/constitution.md`)

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Includes automated multi-tier tests (JUnit 5 unit tests, Testcontainers PostgreSQL concurrency tests, WireMock remote provider tests, and ArchUnit architectural tests) as required by Constitution Principle V.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, build configuration, and architectural boundaries

- [X] T001 Initialize Maven build configuration `pom.xml` with Java 21, Spring Boot 3.3.x dependencies (Spring Web, Spring Data JPA, Spring Security OAuth2 Resource Server, Spring Boot Actuator, Flyway, PostgreSQL, Testcontainers, WireMock, ArchUnit) and Maven Wrapper scripts (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`)
- [X] T002 [P] Configure environment application properties in `src/main/resources/application.yml` and `src/main/resources/application-test.yml`
- [X] T003 [P] Create ArchUnit architecture boundary test suite in `src/test/java/com/deroyque/activation/architecture/ArchitectureRulesTest.java` enforcing Hexagonal Architecture isolation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create database migration script `src/main/resources/db/migration/V1__init_activation_schema.sql` defining `activation_requests` and `activation_outbox` tables, unique constraints, and query indexes
- [X] T005 [P] Implement RFC 9457 Problem Details `@RestControllerAdvice` in `src/main/java/com/deroyque/activation/infrastructure/web/advice/GlobalExceptionHandler.java`
- [X] T006 [P] Implement `CorrelationIdFilter` in `src/main/java/com/deroyque/activation/infrastructure/web/filter/CorrelationIdFilter.java` injecting `traceId` into MDC and response headers
- [X] T007 [P] Implement Spring Security OAuth 2.0 Resource Server configuration with JWT scope converter in `src/main/java/com/deroyque/activation/infrastructure/security/SecurityConfig.java`
- [X] T008 [P] Implement JPA entities `ActivationRequestEntity` and `ActivationOutboxEntity` with `@Version` optimistic locking in `src/main/java/com/deroyque/activation/infrastructure/persistence/entity/`
- [X] T009 [P] Implement Spring Data JPA repositories in `src/main/java/com/deroyque/activation/infrastructure/persistence/repository/SpringDataActivationRepository.java` and `SpringDataOutboxRepository.java`

**Checkpoint**: Foundation ready - user story implementation can begin

---

## Phase 3: User Story 1 - Create Product Activation Request (Priority: P1) 🎯 MVP

**Goal**: Ingest activation requests, validate inputs, persist request in `PENDING` status, and return `201 Created` with `Location` header

**Independent Test**: Submit valid payload to `POST /api/v1/requests` and verify `201 Created` response with valid UUID `id`, `PENDING` status, and `Location` header; submit invalid payload and verify `400 Bad Request` Problem Details.

### Tests for User Story 1
- [X] T010 [P] [US1] Unit test for `ActivationRequest` aggregate creation, validation, and invariants in `src/test/java/com/deroyque/activation/domain/ActivationRequestTest.java`
- [X] T011 [P] [US1] Slice test for `POST /api/v1/requests` testing valid payload, validation rejections, and RFC 9457 errors in `src/test/java/com/deroyque/activation/web/ActivationControllerTest.java`

### Implementation for User Story 1
- [X] T012 [P] [US1] Implement domain aggregate `ActivationRequest`, `ActivationStatus`, and domain exceptions in `src/main/java/com/deroyque/activation/domain/model/`
- [X] T013 [P] [US1] Implement immutable DTO records `CreateActivationRequestDto` and `ActivationResponseDto` with Bean Validation in `src/main/java/com/deroyque/activation/infrastructure/web/dto/`
- [X] T014 [US1] Implement input port `CreateActivationUseCase` and application service `ActivationService.createActivation(...)` in `src/main/java/com/deroyque/activation/application/`
- [X] T015 [US1] Implement output port `ActivationRepositoryPort` and persistence adapter `ActivationRepositoryAdapter` in `src/main/java/com/deroyque/activation/infrastructure/persistence/adapter/`
- [X] T016 [US1] Implement `POST /api/v1/requests` endpoint in `src/main/java/com/deroyque/activation/infrastructure/web/ActivationController.java` returning `201 Created` and `Location` header

**Checkpoint**: User Story 1 is functional as an independent MVP increment

---

## Phase 4: User Story 2 - Idempotent Handling of Duplicate & Concurrent Submissions (Priority: P1)

**Goal**: Guarantee that repeated or concurrent requests with identical `externalId` result in exactly one persistent record, resolving retries to `200 OK` and divergent payloads to `409 Conflict`.

**Independent Test**: Run multi-threaded parallel submissions of identical `externalId` against Testcontainers PostgreSQL asserting 1 record created; submit conflicting payload with same `externalId` asserting `409 Conflict`.

### Tests for User Story 2
- [X] T017 [P] [US2] Concurrency integration test with Testcontainers PostgreSQL verifying 10 parallel threads with same `externalId` result in exactly 1 record in `src/test/java/com/deroyque/activation/persistence/ActivationConcurrencyIntegrationTest.java`

### Implementation for User Story 2
- [X] T018 [US2] Enhance `ActivationService.createActivation(...)` to intercept `DataIntegrityViolationException`, query existing record, and return idempotent `200 OK` on matching payload or throw `ActivationConflictException` on divergent payload
- [X] T019 [US2] Map `ActivationConflictException` in `GlobalExceptionHandler.java` to return RFC 9457 `409 Conflict` Problem Details

**Checkpoint**: User Stories 1 AND 2 work together with verified concurrency safety

---

## Phase 5: User Story 3 - Query Activation Status & History (Priority: P2)

**Goal**: Retrieve single activation request status by ID and support paginated, filtered queries by `customerId` and `status`.

**Independent Test**: Query `/api/v1/requests/{id}` for existing (`200 OK`) and non-existing (`404 Not Found`) IDs; execute filtered paginated queries against `/api/v1/requests?customerId=...&status=...`.

### Tests for User Story 3
- [X] T020 [P] [US3] Web slice test for `GET /api/v1/requests/{id}` and filtered pagination in `src/test/java/com/deroyque/activation/web/ActivationQueryControllerTest.java`

### Implementation for User Story 3
- [X] T021 [P] [US3] Implement `GetActivationUseCase`, `ListActivationsUseCase`, and `PaginatedResponseDto` in `src/main/java/com/deroyque/activation/application/port/in/` and `src/main/java/com/deroyque/activation/infrastructure/web/dto/`
- [X] T022 [US3] Implement pagination and specification query methods in `ActivationRepositoryPort` and `ActivationRepositoryAdapter`
- [X] T023 [US3] Implement `GET /api/v1/requests/{id}` and `GET /api/v1/requests` endpoints in `ActivationController.java`

**Checkpoint**: Status query and paginated search fully functional

---

## Phase 6: User Story 4 - Resilient External Provider Activation (Priority: P2)

**Goal**: Decouple database transactions from HTTP calls via Transactional Outbox, execute remote activation with explicit timeouts, and handle success (200), rejection (400), and retries with backoff (500/timeout).

**Independent Test**: Trigger activation against WireMock simulating 200, 400, 500, and timeout delays; verify status progression (`PENDING` -> `PROCESSING` -> `COMPLETED`/`FAILED`) and retry counter.

### Tests for User Story 4
- [X] T024 [P] [US4] WireMock integration test suite covering 200 OK, 400 Bad Request, 500 Server Error, and read timeout in `src/test/java/com/deroyque/activation/client/ExternalProviderClientIntegrationTest.java`

### Implementation for User Story 4
- [X] T025 [P] [US4] Implement Spring 6 `RestClient` external provider adapter with connect/read timeouts and `X-Idempotency-Key` header in `src/main/java/com/deroyque/activation/infrastructure/client/ExternalProviderClient.java`
- [X] T026 [US4] Implement `OutboxRepositoryPort`, `OutboxRepositoryAdapter`, and atomic outbox record creation inside `ActivationService.createActivation(...)`
- [X] T027 [US4] Implement `OutboxProcessorService` scheduled worker handling asynchronous dispatch, status transitions (`PROCESSING`, `COMPLETED`, `FAILED`), and exponential backoff retry in `src/main/java/com/deroyque/activation/application/service/OutboxProcessorService.java`

**Checkpoint**: Activation workflow reliably decoupled and resilient to downstream latency

---

## Phase 7: User Story 5 - Enterprise Security & Observability (Priority: P3)

**Goal**: Protect API endpoints with OAuth 2.0 Resource Server scope checks and expose production health, metrics, and structured logs.

**Independent Test**: Send requests without token asserting 401; send request with read-only token to POST asserting 403; assert `/actuator/health` and `/actuator/metrics` return operational status.

### Tests for User Story 5
- [X] T028 [P] [US5] Security test suite verifying 401 Unauthorized and 403 Forbidden on write endpoint with read-only scope in `src/test/java/com/deroyque/activation/web/SecurityAccessTest.java`

### Implementation for User Story 5
- [X] T029 [US5] Configure Spring Boot Actuator endpoints, health indicators, Micrometer Prometheus metrics, and Logback JSON logging in `src/main/resources/`

**Checkpoint**: Security and observability gates enforced

---

## Phase 8: Polish, Packaging & Production Readiness

**Purpose**: Containerization, Architecture Decision Records, production incident post-mortem, and documentation

- [X] T030 [P] Create multi-stage, non-root `Dockerfile` with container-aware JVM settings in repository root
- [X] T031 [P] Create `docker-compose.yml` orchestrating Application, PostgreSQL, and WireMock external provider mock
- [X] T032 [P] Document Architecture Decision Records (`docs/adr/ADR-001-idempotency.md`, `docs/adr/ADR-002-outbox.md`, `docs/adr/ADR-003-error-handling.md`)
- [X] T033 [P] Document Production Incident Analysis (`docs/production-incident-analysis.md` answering all questions from challenge item 10)
- [X] T034 Create complete, comprehensive `README.md` containing all required challenge sections (How to run, Technical decisions, Production analysis, Production readiness, Pending points, Future improvements, AI usage)
- [X] T035 Execute full end-to-end verification via `./mvnw clean verify`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion; BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion (MVP!).
- **User Story 2 (Phase 4)**: Depends on US1 completion.
- **User Story 3 (Phase 5)**: Depends on US1 completion (can run parallel to US2/US4).
- **User Story 4 (Phase 6)**: Depends on US1 completion.
- **User Story 5 (Phase 7)**: Depends on Phase 2 & US1 completion.
- **Polish (Phase 8)**: Depends on all user stories completion.

### Parallel Opportunities
- In Phase 1: T002, T003 can run in parallel with T001.
- In Phase 2: T005, T006, T007, T008, T009 can run in parallel.
- Tests marked `[P]` within each user story can be written in parallel before implementation.
- In Phase 8: T030, T031, T032, T033 can be authored in parallel.

---

## Implementation Strategy: MVP First

1. **Step 1**: Complete Phase 1 (Setup) and Phase 2 (Foundational).
2. **Step 2**: Implement Phase 3 (User Story 1) -> Verify standalone MVP (creation and validation working).
3. **Step 3**: Implement Phase 4 (User Story 2) -> Verify concurrency and duplicate protection with Testcontainers.
4. **Step 4**: Implement Phase 5 (User Story 3) -> Verify query and paginated listing.
5. **Step 5**: Implement Phase 6 (User Story 4) -> Verify Outbox and WireMock external provider integration.
6. **Step 6**: Implement Phase 7 (User Story 5) -> Verify Security scopes and Actuator observability.
7. **Step 7**: Complete Phase 8 (Polish & Docs) -> Dockerfile, Docker Compose, ADRs, Incident Analysis, README.md, and `./mvnw verify`.
