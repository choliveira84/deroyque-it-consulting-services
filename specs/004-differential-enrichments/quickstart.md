# Quickstart & Verification Guide: Challenge Differential Enrichments

**Feature**: Challenge Differential Enrichments  
**Branch**: `004-differential-enrichments`  
**Date**: 2026-10-28  

---

## 1. Overview

This guide explains how to verify the documented and configured architectural enrichments covering provider throttling, database query index performance, and security hardening.

---

## 2. Verification Steps

### Step 1: Verify Provider Matrix Documentation
Inspect `README.md` under section **2.2 (Integração Externa)**:
- Confirm presence of the formatted Markdown table with all scenarios:
  - 2xx Success
  - 400 Bad Request
  - 500 Internal Error
  - Network Timeout
  - 429 / 503 with `Retry-After`

### Step 2: Verify Index Strategy Rationale
Inspect `README.md` under section **2.1 (Decisões de Banco e Índices)**:
- Confirm explanation of `idx_activation_requests_cust_status` selectivity ordering.
- Confirm explanation of `idx_activation_requests_created_at` filesort avoidance.
- Confirm explanation of `idx_outbox_status_next_retry` polling optimization.

### Step 3: Verify Security Hardening & Rate Limiting Guidelines
Inspect `README.md` under section **2.3 (Tratamento de Erros e Segurança)** and `src/main/resources/application.yml`:
- Confirm maximum request body size configurations in `application.yml`.
- Confirm BOLA and Token Bucket rate limiting architectural guidelines in `README.md`.

### Step 4: Run Full Test Suite
Ensure zero regression across existing functionality:
```bash
./mvnw clean verify
```
**Expected Outcome**: 28/28 tests pass cleanly.
