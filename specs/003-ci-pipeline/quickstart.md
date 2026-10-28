# Quickstart & Verification Guide: Automated CI Pipeline

**Feature**: Automated CI Pipeline with GitHub Actions  
**Branch**: `003-ci-pipeline`  
**Date**: 2026-10-28  

---

## 1. Overview

This guide explains how to verify the automated CI pipeline locally and understand its behavior when pushed to GitHub.

---

## 2. Local Simulation of the CI Workflow

The CI workflow executes two primary commands on an Ubuntu environment. You can simulate the exact CI sequence on your local development machine:

### Step 1: Run Full Clean Verification & Test Suite
```bash
./mvnw clean verify
```
**Expected Outcome**:
- All 28 tests pass (0 failures, 0 errors, 0 skipped).
- ArchUnit architectural rules pass.
- Packaged executable JAR is produced at `target/product-activation-service-0.0.1-SNAPSHOT.jar`.
- Total time: ~1m30s to 2m.

### Step 2: Validate Dockerfile Packaging
```bash
docker build -t product-activation-service:ci-test .
```
**Expected Outcome**:
- Multi-stage Docker build completes with exit code 0.
- Docker image `product-activation-service:ci-test` is created.

---

## 3. Remote Verification on GitHub

When code is pushed to GitHub:

1. **Push Branch**:
   ```bash
   git push origin 003-ci-pipeline
   ```
2. **Access GitHub Actions**:
   - Go to `https://github.com/<owner>/<repo>/actions`.
   - Click on the **CI** workflow.
   - Inspect the live job steps:
     1. `actions/checkout@v4`
     2. `actions/setup-java@v4` (JDK 21 Temurin + cache)
     3. Make Maven Wrapper executable
     4. Build and run tests with Maven (`./mvnw clean verify`)
     5. Build Docker image (`docker build`)
3. **Verify Status Badge**:
   - Open `README.md` on the repository root.
   - The badge should display a green `passing` status.
