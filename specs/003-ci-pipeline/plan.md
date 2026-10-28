# Implementation Plan: Automated CI Pipeline with GitHub Actions

**Branch**: `003-ci-pipeline` | **Date**: 2026-10-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-ci-pipeline/spec.md`

---

## Summary

Implement an automated Continuous Integration (CI) pipeline using GitHub Actions to act as an automated quality gate. The workflow triggers on pushes and pull requests to `main` and feature branches, provisions an `ubuntu-latest` runner with Eclipse Temurin JDK 21 and Maven caching, executes the full verification and test suite (`./mvnw clean verify` running unit, ArchUnit, and integration tests), and validates multi-stage Docker build packaging. In addition, embed a dynamic CI status badge in `README.md` and document the CI/CD architecture and cloud production deployment roadmap.

---

## Technical Context

**Language/Version**: Java 21 (LTS), YAML (GitHub Actions workflow syntax)  
**Primary Dependencies**:
- GitHub Actions: `actions/checkout@v4`, `actions/setup-java@v4`
- Maven Wrapper (`mvnw`)
- Docker Engine (provided by `ubuntu-latest` runner)

**Storage**: N/A (database changes not required; Testcontainers spins up PostgreSQL during CI tests)  
**Testing**:
- Maven Surefire / Failsafe executing 28 automated tests (domain unit tests, ArchUnit, Testcontainers, WireMock, WebMvc)
- Docker smoke build test (`docker build`)

**Target Platform**: GitHub Actions runner (`ubuntu-latest`)  
**Project Type**: CI/CD automation & DevOps documentation  
**Performance Goals**: Workflow execution < 3 minutes; cache hit speedup > 50%  
**Constraints**: Zero cost (uses free public runner pool / free tier quota); zero changes to Java domain or application business logic; 100% adherence to project constitution  
**Scale/Scope**: 1 workflow file (`.github/workflows/ci.yml`), 1 README badge/section update  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement | Status | Verification Plan |
|---|---|---|---|
| **I. Simplicity & Architectural Boundaries** | Domain logic isolated; no framework leak | **PASS** | CI configuration resides strictly in `.github/workflows/` and documentation; zero impact on Java source code. |
| **II. Concurrency Control & Idempotency** | No changes to domain concurrency logic | **PASS** | Existing concurrency controls untouched; CI validates them via `ActivationConcurrencyIntegrationTest`. |
| **III. Transaction-Remote Decoupling** | No remote calls in DB transactions | **PASS** | Outbox pattern and decoupling unchanged; verified by CI tests. |
| **IV. Standardized Error Handling & Security** | RFC 9457 & OAuth 2.0 integrity maintained | **PASS** | Web security and error handling tests validated by CI. |
| **V. Comprehensive Multi-Tier Testing** | Automated tests MUST pass cleanly via `./mvnw verify` | **PASS** | Core purpose of the CI pipeline is enforcing `./mvnw verify` as an unbypassable gatekeeper. |

---

## Project Structure

### Documentation (this feature)

```text
specs/003-ci-pipeline/
├── plan.md              # Implementation plan (this document)
├── research.md          # Phase 0: CI platform, caching, triggers, test execution
├── quickstart.md        # Phase 1: Local simulation & GitHub Actions verification
└── tasks.md             # Phase 2: Actionable implementation tasks (speckit-tasks)
```

### Source Code / Automation Files

```text
.github/
└── workflows/
    └── ci.yml           # GitHub Actions workflow definition

README.md                # CI status badge and CI/CD architecture documentation
```

---

## Phase 0: Research Summary

All research decisions documented in [`research.md`](./research.md):
- **Runner**: `ubuntu-latest` with native Docker daemon.
- **JDK & Caching**: `actions/setup-java@v4` with `distribution: 'temurin'`, `java-version: '21'`, and `cache: 'maven'`.
- **Concurrency**: `cancel-in-progress: true` keyed on `${{ github.workflow }}-${{ github.ref }}`.
- **Verification**: `./mvnw --batch-mode clean verify` + `docker build -t product-activation-service:test .`.

---

## Phase 1: Design & Verification Guide

All verification scenarios and setup steps documented in [`quickstart.md`](./quickstart.md).
Zero database or entity model changes required.
Zero API contract modifications required.
