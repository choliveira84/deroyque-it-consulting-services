<!--
Sync Impact Report:
- Version change: Uninitialized template -> 1.0.0
- Bump rationale: Initial ratification of the project constitution based on the technical challenge requirements and architectural analysis.
- Modified principles: Replaced all placeholder template principles with domain-specific engineering principles.
- Added sections:
  * Technology Stack & Operational Constraints
  * Development Workflow, Review Process & Production Readiness
- Removed sections: None
- Follow-up TODOs: None
-->

# Deroyque Product Activation Service Constitution

## Core Principles

### I. Simplicity & Architectural Boundaries (Hexagonal & Modern Java)
The codebase MUST prioritize clarity, maintainability, and clean separation of concerns over unnecessary abstraction layers or excessive design patterns (YAGNI).
- Domain logic MUST remain isolated from frameworks, external protocols, and database entities; architectural boundaries MUST be verified via ArchUnit.
- Native Java 21 features MUST be preferred: use Java `record` types for all immutable DTOs and command inputs, pattern matching, and sealed types where state modeling demands exhaustiveness.
- Third-party code generation libraries (such as Lombok) MUST NOT be introduced when native Java records and standard constructs suffice.
- DTOs MUST be strictly decoupled from JPA entities.

### II. Concurrency Control & Idempotency (NON-NEGOTIABLE)
The application MUST prevent duplicate product activations and concurrent state corruption under high contention.
- The `externalId` is a unique client-supplied idempotency key. A unique database constraint on `externalId` MUST be enforced at the schema level.
- Application code MUST NOT rely solely on "check-then-insert" logic; race conditions MUST be caught and resolved predictably.
- Parallel requests with identical `externalId` and matching payload MUST result in exactly one persistent activation request; the duplicate call MUST receive an idempotent response (HTTP 200 OK with the existing record, or HTTP 409 Conflict if payload diverges).
- State transitions (`PENDING` -> `PROCESSING` -> `COMPLETED` / `FAILED`) MUST be strictly encapsulated and validated in the domain model. Entities MUST utilize optimistic locking (`@Version`) to prevent lost updates during concurrent processing.

### III. Transaction-Remote Call Decoupling (Resilience & Outbox)
Remote HTTP network calls MUST NEVER be executed inside active relational database transactions.
- The database transaction saving or updating the request MUST commit before or independently of the external HTTP call (`POST /provider/v1/activate`) to prevent connection pool exhaustion and cascading latency.
- Reliable delivery MUST be guaranteed using asynchronous processing with the Transactional Outbox pattern or event-driven dispatch.
- HTTP client integrations (`RestClient`) MUST define explicit connection and read timeouts.
- Outgoing provider requests MUST propagate an idempotency key to protect against duplicate downstream activations during network timeouts and retries.
- A defined retry backoff strategy and reconciliation mechanism MUST exist for requests interrupted in `PROCESSING` status.

### IV. Standardized Error Handling & Security (RFC 9457 & OAuth 2.0 / JWT)
The API MUST expose safe, predictable, and standards-compliant error responses while protecting resources with enterprise-grade security.
- Internal stack traces, raw SQL messages, and system internals MUST NEVER be returned to consumers.
- All error responses MUST adhere to RFC 9457 (`application/problem+json`) using Spring's `ProblemDetail`, enriched with correlation identifiers (`traceId`) and validation error details.
- Endpoints MUST be secured as an OAuth 2.0 Resource Server using JWT validation (validating signature, issuer, audience, and expiration).
- Scopes MUST strictly segregate access (e.g., `SCOPE_activations:write` for activation creation, `SCOPE_activations:read` for querying).
- Protection against Broken Object Level Authorization (BOLA) MUST be planned, ensuring clients can only query their authorized requests.

### V. Comprehensive Multi-Tier Testing (Quality Gates)
Automated testing is mandatory and acts as the gatekeeper for all changes. Tests MUST provide verifiable confidence rather than superficial coverage.
- Domain unit tests (JUnit 5, AssertJ, Mockito) MUST exhaustively test valid and invalid state transitions without Spring context overhead.
- Concurrency and duplicate-request integration tests MUST run against real PostgreSQL using Testcontainers, verifying race conditions across parallel threads.
- Remote provider interactions MUST be validated using WireMock, testing success (2xx), client errors (400), server errors (500), and timeout/delay scenarios.
- Security tests (`@WebMvcTest`) MUST verify 401 Unauthorized (missing/invalid token) and 403 Forbidden (insufficient scope).
- All automated tests MUST execute cleanly in a single command: `./mvnw verify`.

## Technology Stack & Operational Constraints

- **Language & Runtime**: Java 21 (LTS) running in modern containerized environments.
- **Framework**: Spring Boot 3.x (Spring MVC, Spring Data JPA, Spring Security Resource Server, Spring Boot Actuator).
- **Persistence & Migration**: PostgreSQL with Flyway for version-controlled, repeatable database schema migrations.
- **Build Tool**: Maven with Maven Wrapper (`./mvnw`).
- **Containerization**: Multi-stage, non-root `Dockerfile` optimized for minimal attack surface and container-aware JVM memory settings; `docker-compose.yml` orchestrating App, PostgreSQL, and external provider mock.
- **Observability**: Spring Boot Actuator (health, readiness, liveness probes), structured JSON logging with MDC trace propagation, and Micrometer metrics.

## Development Workflow, Review Process & Production Readiness

- **Commit Discipline**: Atomic, intentional commits adhering to Conventional Commits format (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`).
- **Architecture Decision Records (ADRs)**: Significant architectural choices (idempotency strategy, outbox pattern, error format, security model) MUST be documented under `docs/adr/`.
- **Quality Gates**: Every pull request MUST pass `./mvnw verify` without warnings or flaky tests.
- **Production Latency Post-Mortem**: Document root-cause analysis and remediation strategies for connection pool exhaustion, cascading downstream delays, and slow query profiling in `README.md`.

## Governance

- This Constitution establishes the foundational engineering principles and technical contract for the project.
- Any amendment requires updating this document, justifying the version bump, and aligning affected architectural artifacts and ADRs.
- All code reviews and implementation tasks MUST verify adherence to these core principles before merging.

**Version**: 1.0.0 | **Ratified**: 2026-10-28 | **Last Amended**: 2026-10-28
