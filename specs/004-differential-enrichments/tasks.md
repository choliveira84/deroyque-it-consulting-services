---
description: "Implementation tasks for Challenge Differential Enrichments"
---

# Tasks: Challenge Differential Enrichments

**Input**: Design documents from `/specs/004-differential-enrichments/`  
**Prerequisites**: [plan.md](./plan.md) (required), [spec.md](./spec.md) (required), [research.md](./research.md), [quickstart.md](./quickstart.md)  
**Organization**: Tasks are grouped by phase and user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths are specified in each task

---

## Phase 1: Setup & Baseline

**Purpose**: Validate system baseline before applying enrichments.

- [x] T001 Run baseline verification with `./mvnw clean verify` ensuring all 28 tests pass

---

## Phase 2: User Story 1 - Provider Throttling & Retry-After Matrix (Priority: P1) 🎯 MVP

**Goal**: Formally document the complete external provider response behavior matrix in `README.md`, including `HTTP 429 / 503 com Retry-After`.

**Independent Test**: Review `README.md` section 2.2 to verify the matrix format, status transitions, and retry justification for all HTTP scenarios.

### Implementation for User Story 1

- [x] T002 [US1] Add structured Provider Lifecycle Matrix table in `README.md` (Section 2.2), covering 2xx, 400, 500, Timeout, and 429/503 with `Retry-After`
- [x] T003 [US1] Document downstream throttling protection, Thundering Herd avoidance, and delay calculation with jitter in `README.md` (Section 2.2)

**Checkpoint**: User Story 1 complete and independently verifiable.

---

## Phase 3: User Story 2 - Database Indexing Strategy and Rationale (Priority: P1)

**Goal**: Formally document and justify the database indexes implemented in PostgreSQL to demonstrate query optimization rigor.

**Independent Test**: Review `README.md` section 2.1 to confirm the technical rationale behind selectivity ordering, filesort elimination, and outbox polling efficiency.

### Implementation for User Story 2

- [x] T004 [US2] Add comprehensive Database Indexing Strategy in `README.md` (Section 2.1), explaining `idx_activation_requests_cust_status` cardinality and prefix matching
- [x] T005 [US2] Document filesort avoidance on `created_at DESC` and outbox polling optimization on `(status, next_retry_at)` in `README.md` (Section 2.1)

**Checkpoint**: User Story 2 complete and independently verifiable.

---

## Phase 4: User Story 3 - API Hardening: Rate Limiting & BOLA Prevention (Priority: P2)

**Goal**: Configure request body limits in Spring Boot and document Rate Limiting and BOLA protection patterns.

**Independent Test**: Inspect `src/main/resources/application.yml` and `README.md` section 2.3 to verify payload limits and security architecture.

### Implementation for User Story 3

- [x] T006 [P] [US3] Configure max HTTP request body and multipart size limits in `src/main/resources/application.yml`
- [x] T007 [US3] Document Token Bucket Rate Limiting architecture and OWASP BOLA tenant validation in `README.md` (Section 2.3)

**Checkpoint**: All user stories complete and documented.

---

## Phase 5: Polish & Final Verification

**Purpose**: Execute full automated test suite and synchronize branch.

- [x] T008 Execute `./mvnw clean verify` to ensure zero regression across all 28 automated tests
- [x] T009 Commit all changes using Conventional Commits and push branch `004-differential-enrichments`

---

## Dependencies & Execution Order

- **Phase 1**: Baseline validation.
- **Phase 2 (US1)**: Provider matrix documentation in `README.md`.
- **Phase 3 (US2)**: Database indexing documentation in `README.md`.
- **Phase 4 (US3)**: Config update in `application.yml` and security documentation in `README.md`.
- **Phase 5**: Full test execution and git push.
