# Feature Specification: Automated CI Pipeline with GitHub Actions

**Feature Branch**: `003-ci-pipeline`  
**Created**: 2026-10-28  
**Status**: Draft  
**Input**: User description: "vamos falar sobre o pipeline simples, que é um diferencial. sim, vamos criar! mas via specification, ok?"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automated Build and Verification Quality Gate (Priority: P1)

As a software engineer, code reviewer, or technical evaluator,  
I want GitHub Actions to automatically run on every push and pull request,  
So that the application is built from scratch with Java 21, all 28 automated tests (domain unit tests, ArchUnit hexagonal rules, integration tests with Testcontainers/WireMock, and OpenAPI integration tests) are executed, and any regression or broken architectural rule is caught immediately before merging.

**Why this priority**:
Automated CI is a primary differentiator in enterprise engineering and a specified evaluation differentiator for this project. It ensures that "it works on my machine" is elevated to a verified, reproducible guarantee across all branches and pull requests.

**Independent Test**:
Push a commit or open a pull request on GitHub. The GitHub Actions runner provisions an `ubuntu-latest` VM, sets up Java 21 Temurin with Maven dependency caching, runs `./mvnw --batch-mode clean verify`, and reports a green checkmark (exit code 0) within 3 minutes.

**Acceptance Scenarios**:
1. **Given** any commit pushed to a branch or pull request targeting `main`, **When** the GitHub Actions workflow triggers, **Then** it checks out the repository, configures JDK 21 Temurin, and caches Maven dependencies in `~/.m2/repository`.
2. **Given** the CI runner environment, **When** `./mvnw clean verify` executes, **Then** all domain unit tests, Spring Boot integration tests, and ArchUnit architecture compliance rules run and pass without failures or errors.
3. **Given** a pull request with broken code or architectural violations, **When** CI runs, **Then** the workflow fails with an informative error log and blocks merge.

---

### User Story 2 - Container Packaging Verification (Priority: P2)

As a DevOps engineer or code reviewer,  
I want the CI workflow to build the production Docker image using the project's multi-stage `Dockerfile`,  
So that any regression in container packaging, alpine package dependencies, or layer caching is validated prior to deployment.

**Why this priority**:
Catching Docker build errors in CI guarantees that production deploy artifacts are always ship-ready and not just valid as local JAR files.

**Independent Test**:
In the CI workflow after the Maven verification step, execute `docker build -t product-activation-service:test .`. The multi-stage build succeeds and produces a valid container image.

**Acceptance Scenarios**:
1. **Given** a successful Maven build step in CI, **When** the container build step runs, **Then** Docker builds the image using the multi-stage `Dockerfile` and exits with code 0.
2. **Given** a change to the `Dockerfile` that introduces a syntax or packaging error, **When** CI runs, **Then** the Docker build step fails and alerts the developer.

---

### User Story 3 - Repository Status Badge and CI/CD Cloud Documentation (Priority: P3)

As a technical evaluator or repository visitor,  
I want to see a CI status badge at the top of the `README.md` and an explanatory section describing the CI/CD pipeline architecture and future cloud deployment roadmap,  
So that the repository showcases operational excellence and answers the challenge's technical questions regarding continuous integration and cloud deployment.

**Why this priority**:
Fulfills the challenge requirement regarding technical documentation and transparency of build status.

**Independent Test**:
View `README.md` on GitHub: the workflow badge dynamically displays the build status (`passing`), and the CI/CD section documents the pipeline stages and cloud deployment strategy (build, container registry, and cloud orchestrator).

**Acceptance Scenarios**:
1. **Given** the repository on GitHub, **When** viewing the `README.md`, **Then** a dynamic status badge displays the current CI build status of the `main` branch.
2. **Given** the technical documentation in `README.md`, **When** reviewed by an evaluator, **Then** it clearly explains the CI workflow steps and how it evolves into a full CD pipeline up to cloud deployment (ECR/Artifact Registry, Kubernetes/ECS/Cloud Run).

---

### Edge Cases

- **Fast successive commits**: When multiple commits are pushed in rapid succession, earlier in-flight builds must be automatically cancelled using GitHub Actions concurrency groups (`cancel-in-progress: true`) to avoid wasting runner minutes.
- **Maven wrapper execution permissions**: On Linux runners, `mvnw` must have execute permissions (`chmod +x mvnw`) configured explicitly in the workflow step.
- **Cache miss on new dependencies**: When `pom.xml` changes, the Maven cache action must smoothly download new dependencies from Maven Central without failing the build.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST define a GitHub Actions workflow file at `.github/workflows/ci.yml`.
- **FR-002**: The workflow MUST trigger automatically on `push` to `main` and feature branches, and on `pull_request` targeting `main`.
- **FR-003**: The workflow MUST run on `ubuntu-latest` and provision Eclipse Temurin JDK 21.
- **FR-004**: The workflow MUST enable Maven dependency caching (`cache: 'maven'`) in the `actions/setup-java` step.
- **FR-005**: The workflow MUST ensure `./mvnw` has executable permissions (`chmod +x ./mvnw`).
- **FR-006**: The workflow MUST execute `./mvnw --batch-mode clean verify` to run the entire test suite and packaging.
- **FR-007**: The workflow MUST validate container buildability by running `docker build` against the root `Dockerfile`.
- **FR-008**: The workflow MUST define a concurrency group with `cancel-in-progress: true` keyed by workflow and git ref.
- **FR-009**: The repository `README.md` MUST include a GitHub Actions CI badge linking to the workflow.
- **FR-010**: The repository `README.md` MUST include a dedicated CI/CD section detailing the automated pipeline and addressing the cloud deployment path (build, container registry, and deployment).

---

## Key Entities *(CI Workflow Components)*

- **CI Workflow (`ci.yml`)**: Root orchestration definition specifying triggers, permissions, jobs, and runner environments.
- **Build & Test Job (`build`)**: Primary CI job executing checkout, JDK setup, Maven verification, and test execution.
- **Container Smoke Test Job / Step (`docker`)**: Step verifying the multi-stage Docker build integrity.
- **Status Badge**: SVG indicator embedded in `README.md` rendering the real-time health of the CI workflow.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Workflow YAML syntax validates successfully against the GitHub Actions schema without warnings.
- **SC-002**: Complete CI run finishes in under 3 minutes on standard GitHub-hosted runners.
- **SC-003**: 100% of automated tests (28 unit, architecture, and integration tests) pass in the CI environment.
- **SC-004**: Multi-stage Docker image builds cleanly in the CI runner without requiring pre-cached local images.
- **SC-005**: `README.md` provides complete documentation on the CI pipeline and its cloud deployment extension.

---

## Assumptions

- The repository will be hosted on GitHub, which provides standard GitHub Actions runners (`ubuntu-latest`) at zero cost for public repositories and 2,000 free minutes/month for private repositories.
- Testcontainers tests inside the CI runner require Docker, which is pre-installed on `ubuntu-latest` GitHub-hosted runners.
- The pipeline intentionally focuses on CI (Continuous Integration: build, test, package, docker build) as required by the challenge scope, while documenting the CD (Continuous Deployment) path conceptually in the `README.md`.
