# Implementation Plan: Customer Product Activation Service

**Branch**: `001-product-activation` | **Date**: 2026-10-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-product-activation/spec.md`

## Summary

Build a high-performance, fault-tolerant Customer Product Activation Service in Java 21 and Spring Boot 3.x. The service ingests activation requests via REST, strictly prevents duplicate requests (`externalId`) across concurrent threads using database constraints and optimistic locking, and decouples relational database transactions from external provider HTTP calls using the Transactional Outbox pattern. All error responses conform to RFC 9457 Problem Details with trace correlation, secured via OAuth 2.0 Resource Server JWT validation, and verified across all tiers with Testcontainers, WireMock, and ArchUnit.

## Technical Context

**Language/Version**: Java 21 (LTS) with native `record` types, pattern matching, and sealed types.

**Primary Dependencies**: Spring Boot 3.3.x (Spring MVC, Spring Data JPA, Spring Security OAuth2 Resource Server, Spring Boot Actuator, Flyway Migration).

**Storage**: PostgreSQL 16 (production & Testcontainers integration tests).

**Testing**: JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL), WireMock, ArchUnit, Spring Boot Test (`MockMvc`, `@WebMvcTest`, `@DataJpaTest`).

**Target Platform**: Linux container (multi-stage Docker, distroless/temurin non-root, Docker Compose).

**Project Type**: Spring Boot REST microservice.

**Performance Goals**:
- P95 ingestion response time < 100 ms.
- 100% isolation of DB connection pool during downstream external provider latency (up to 3.0 s).
- Exactly 1 record created when N concurrent identical requests hit the service at the exact same millisecond.

**Constraints**:
- Never execute remote HTTP calls inside an active database transaction.
- Zero stack trace leakage to API clients (RFC 9457 Problem Details with MDC `traceId`).
- Single-command build and multi-tier verification (`./mvnw verify`).

**Scale/Scope**: Enterprise-ready product activation service handling thousands of daily activations with audit trail and resilience.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Principle I: Simplicity & Architectural Boundaries (Hexagonal & Modern Java)**: Clean Hexagonal architecture (`domain`, `application`, `infrastructure`), DTOs as Java records, no Lombok, validated via ArchUnit.
- [x] **Principle II: Concurrency Control & Idempotency (NON-NEGOTIABLE)**: Enforced via `externalId` unique constraint, optimistic locking (`@Version`), idempotent replay on retry (200 OK vs 409 Conflict), domain-governed state machine.
- [x] **Principle III: Transaction-Remote Call Decoupling (Resilience & Outbox)**: Ingestion commits request + outbox atomically in < 10 ms; asynchronous outbox worker handles `POST /provider/v1/activate` outside DB transactions with explicit timeouts and idempotency key.
- [x] **Principle IV: Standardized Error Handling & Security (RFC 9457 & OAuth 2.0 / JWT)**: Global `@RestControllerAdvice` returning `ProblemDetail` with `traceId`; Spring Security Resource Server validating JWT scopes (`activations:read`, `activations:write`).
- [x] **Principle V: Comprehensive Multi-Tier Testing (Quality Gates)**: Pure domain unit tests, Testcontainers concurrency integration tests, WireMock remote provider failure/timeout tests, ArchUnit architectural tests, executable via `./mvnw verify`.

*All gates passed cleanly with zero violations.*

## Project Structure

### Documentation (this feature)

```text
specs/001-product-activation/
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0: Concurrency, outbox, RFC 9457 & security decisions
├── data-model.md        # Phase 1: Entity model, state machine, Flyway migration & indexes
├── quickstart.md        # Phase 1: Build, run with Docker Compose & cURL verification
├── contracts/           # Phase 1: OpenAPI contracts
│   ├── api-v1-requests.yaml
│   └── provider-v1-activate.yaml
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
deroyque-it-consulting-services/
├── pom.xml
├── mvnw / mvnw.cmd
├── Dockerfile
├── docker-compose.yml
├── README.md
├── docs/
│   ├── adr/
│   │   ├── ADR-001-idempotency-and-deduplication.md
│   │   ├── ADR-002-transactional-outbox-pattern.md
│   │   └── ADR-003-rfc9457-problem-details.md
│   └── production-incident-analysis.md
└── src/
    ├── main/
    │   ├── java/com/deroyque/activation/
    │   │   ├── ProductActivationApplication.java
    │   │   ├── domain/                                  # Pure Domain (No framework deps)
    │   │   │   ├── model/
    │   │   │   │   ├── ActivationRequest.java
    │   │   │   │   └── ActivationStatus.java
    │   │   │   └── exception/
    │   │   │       ├── ActivationConflictException.java
    │   │   │       ├── ActivationNotFoundException.java
    │   │   │       └── InvalidStateTransitionException.java
    │   │   ├── application/                             # Application Use Cases & Outbox
    │   │   │   ├── port/in/
    │   │   │   │   ├── CreateActivationUseCase.java
    │   │   │   │   ├── GetActivationUseCase.java
    │   │   │   │   └── ListActivationsUseCase.java
    │   │   │   ├── port/out/
    │   │   │   │   ├── ActivationRepositoryPort.java
    │   │   │   │   ├── OutboxRepositoryPort.java
    │   │   │   │   └── ExternalProviderPort.java
    │   │   │   └── service/
    │   │   │       ├── ActivationService.java
    │   │   │       └── OutboxProcessorService.java
    │   │   └── infrastructure/                          # Adapters & Framework Config
    │   │       ├── web/
    │   │       │   ├── ActivationController.java
    │   │       │   ├── dto/
    │   │       │   │   ├── CreateActivationRequestDto.java   # Java record
    │   │       │   │   ├── ActivationResponseDto.java        # Java record
    │   │       │   │   └── PaginatedResponseDto.java         # Java record
    │   │       │   ├── filter/
    │   │       │   │   └── CorrelationIdFilter.java
    │   │       │   └── advice/
    │   │       │       └── GlobalExceptionHandler.java       # RFC 9457 ProblemDetail
    │   │       ├── persistence/
    │   │       │   ├── entity/
    │   │       │   │   ├── ActivationRequestEntity.java
    │   │       │   │   └── ActivationOutboxEntity.java
    │   │       │   ├── repository/
    │   │       │   │   ├── SpringDataActivationRepository.java
    │   │       │   │   └── SpringDataOutboxRepository.java
    │   │       │   └── adapter/
    │   │       │       ├── ActivationRepositoryAdapter.java
    │   │       │       └── OutboxRepositoryAdapter.java
    │   │       ├── client/
    │   │       │   ├── ExternalProviderClient.java           # Spring RestClient
    │   │       │   └── config/
    │   │       │       └── RestClientConfig.java
    │   │       ├── security/
    │   │       │   └── SecurityConfig.java                   # OAuth 2.0 Resource Server
    │   │       └── config/
    │   │           └── AsyncConfig.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-test.yml
    │       └── db/migration/
    │           └── V1__init_activation_schema.sql
    └── test/
        ├── java/com/deroyque/activation/
        │   ├── domain/                                  # Fast POJO unit tests
        │   │   └── ActivationRequestTest.java
        │   ├── persistence/                             # Concurrency tests (Testcontainers)
        │   │   └── ActivationConcurrencyIntegrationTest.java
        │   ├── client/                                  # Remote integration tests (WireMock)
        │   │   └── ExternalProviderClientIntegrationTest.java
        │   ├── web/                                     # Security & RFC 9457 slice tests
        │   │   ├── ActivationControllerTest.java
        │   │   └── SecurityAccessTest.java
        │   └── architecture/                            # Boundary verification (ArchUnit)
        │       └── ArchitectureRulesTest.java
        └── resources/
            └── wiremock/
```

**Structure Decision**: A clean single-project Maven repository organized along Hexagonal / Ports & Adapters architectural principles. It completely isolates the business domain from Spring/JPA, provides clear application use case boundaries, and implements infrastructure adapters for Web, Database, and Remote Provider.

## Complexity Tracking

*No unjustified complexity or constitutional violations.*
- **Outbox Pattern**: Essential to prevent connection pool exhaustion during downstream provider latency (directly resolving the production incident described in challenge item 10).
- **ArchUnit**: Verifies architectural integrity in CI automatically.
