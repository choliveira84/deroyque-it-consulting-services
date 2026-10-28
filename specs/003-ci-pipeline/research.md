# Research & Technical Decisions: Automated CI Pipeline with GitHub Actions

**Feature**: Automated CI Pipeline with GitHub Actions  
**Branch**: `003-ci-pipeline`  
**Date**: 2026-10-28  

---

## 1. CI Platform Selection

### Decision
Use **GitHub Actions** with standard `ubuntu-latest` runners.

### Rationale
- Native integration with GitHub repositories: zero external webhook or service setup.
- 100% free and unlimited execution minutes for public repositories; 2,000 free minutes/month for private repositories.
- Docker daemon is pre-installed and running on `ubuntu-latest` runners, allowing Testcontainers tests and `docker build` smoke tests to run without extra service configuration.
- Broadest industry adoption for open-source Java/Spring Boot projects.

### Alternatives Considered
- *GitLab CI*: Requires hosting on GitLab or self-hosting runners.
- *Jenkins*: Heavy maintenance overhead, requires dedicated server/compute infrastructure.
- *CircleCI / Travis CI*: Adds third-party authorization and external dashboard dependencies.

---

## 2. JDK Setup & Dependency Caching Strategy

### Decision
Use `actions/setup-java@v4` configuring:
- `distribution: 'temurin'` (Eclipse Temurin JDK 21 LTS, matching the production Dockerfile)
- `java-version: '21'`
- `cache: 'maven'`

### Rationale
- `actions/setup-java@v4` provides built-in, native hash-based caching of `~/.m2/repository` keyed on `pom.xml`.
- Reduces average build time by 60–75% after the first run by reusing downloaded Maven plugins and libraries.
- Temurin 21 matches our exact production runtime and compiler target.

### Alternatives Considered
- *Manual `actions/cache@v4`*: Adds verbose configuration prone to key drift compared to the native `cache: 'maven'` option in `setup-java`.

---

## 3. Workflow Triggers & Concurrency Optimization

### Decision
Configure triggers on:
- `push` to `main` and feature branches (`00*`, `feat/**`, etc.)
- `pull_request` targeting `main`

Configure concurrency group:
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

### Rationale
- Runs on every PR to ensure all quality gates pass prior to merge.
- `cancel-in-progress: true` automatically cancels stale runs when new commits are pushed to the same PR/branch, saving runner time and providing immediate feedback on the latest state.

---

## 4. Test Suite Execution & Testcontainers Compatibility

### Decision
Execute `./mvnw --batch-mode clean verify`.

### Rationale
- `./mvnw verify` runs all surefire test classes:
  - Unit tests: `ActivationRequestTest`
  - Hexagonal Architecture tests: `ArchitectureRulesTest` (ArchUnit)
  - Integration tests: `ExternalProviderClientIntegrationTest`, `ActivationConcurrencyIntegrationTest`, `ActivationQueryControllerTest`, `OpenApiDocsIntegrationTest`
- GitHub-hosted `ubuntu-latest` runners have rootless Docker available, which Testcontainers connects to automatically via `/var/run/docker.sock`.
- Running in `--batch-mode` suppresses download progress bars, keeping CI logs compact and readable.

---

## 5. Docker Packaging Smoke Test

### Decision
Include a `docker build` step after `mvnw verify`:
```bash
docker build -t product-activation-service:test .
```

### Rationale
- Verifies that our multi-stage `Dockerfile` (JDK builder + JRE alpine runner + non-root user) builds cleanly and produces an executable image.
- Fulfills the challenge requirement demonstrating container buildability in CI.

---

## 6. README Badge and Architectural Documentation

### Decision
Add a status badge at the top of `README.md`:
```markdown
[![CI](https://github.com/<owner>/<repo>/actions/workflows/ci.yml/badge.svg)](https://github.com/<owner>/<repo>/actions/workflows/ci.yml)
```
And add a dedicated section in `README.md` explaining:
1. CI Quality Gate stages.
2. Architecture of full production CD (Container Registry, Kubernetes/ECS, GitOps).
