# Implementation Plan: Challenge Differential Enrichments

**Branch**: `004-differential-enrichments` | **Date**: 2026-10-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-differential-enrichments/spec.md`

---

## Summary

Incorporate the final bonus differential criteria outlined in the technical evaluation guide into the project:
1. Provide the comprehensive provider return behavior matrix in `README.md`, explicitly defining lifecycle transitions and retry delay calculations for `HTTP 429 Too Many Requests` and `HTTP 503 Service Unavailable` with `Retry-After`.
2. Formally document and justify the database indexing strategy in `README.md`, explaining query planning, B-Tree selectivity ordering (`customer_id`, `status`), and outbox polling efficiency.
3. Configure application payload limits in `application.yml` and document API security hardening (Token Bucket Rate Limiting, OWASP BOLA prevention).

---

## Technical Context

**Language/Version**: Java 21 (LTS), YAML  
**Primary Dependencies**:
- Spring Boot 3.3.5
- PostgreSQL 16 (B-Tree indexing)
- Flyway Database Migrations
- Spring Security 6.x

**Storage**: PostgreSQL (`idx_activation_requests_cust_status`, `idx_activation_requests_created_at`, `idx_outbox_status_next_retry`)  
**Testing**:
- Maven Surefire executing 28 automated tests (domain unit tests, ArchUnit, Testcontainers, WireMock, WebMvc)

**Target Platform**: Docker container & local development  
**Project Type**: Spring Boot Web REST microservice  
**Performance Goals**: Sub-50ms indexed query scans on multi-million row datasets  
**Constraints**: Zero regression; 100% backward compatibility; all existing 28 tests must remain passing  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement | Status | Verification Plan |
|---|---|---|---|
| **I. Simplicity & Architectural Boundaries** | Domain logic isolated; clean separation | **PASS** | Enrichments focus on documentation, application properties, and performance justifications without modifying domain core. |
| **II. Concurrency Control & Idempotency** | No changes to concurrency mechanics | **PASS** | Reinforces the justification of unique constraints and atomic outbox processing. |
| **III. Transaction-Remote Decoupling** | No remote calls in DB transactions | **PASS** | Formalizes downstream throttling semantics (`Retry-After`) handled exclusively by the outbox processor. |
| **IV. Standardized Error Handling & Security** | RFC 9457 & OAuth 2.0 integrity | **PASS** | Adds explicit request size limits and BOLA defense patterns. |
| **V. Comprehensive Multi-Tier Testing** | Automated tests pass cleanly | **PASS** | `./mvnw clean verify` verified against all changes. |

---

## Project Structure

### Documentation (this feature)

```text
specs/004-differential-enrichments/
├── plan.md              # Implementation plan (this document)
├── research.md          # Phase 0: Throttling, index selectivity, rate limiting
├── quickstart.md        # Phase 1: Verification guide
└── tasks.md             # Phase 2: Actionable implementation tasks (speckit-tasks)
```

### Source Code / Configuration Files

```text
src/main/resources/
└── application.yml      # Request body limit configurations

README.md                # Provider matrix, database indexing rationale, security hardening
```
