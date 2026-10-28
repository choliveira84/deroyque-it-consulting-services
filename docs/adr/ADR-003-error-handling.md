# ADR-003: RFC 9457 Problem Details & Security Architecture

## Status
Accepted

## Context
Standardizing error communication across services is essential for debugging and consumer clarity, while security requirements forbid leaking internal database structures or Java stack traces.

## Decision
1. **RFC 9457 Format**: All error responses return `application/problem+json` using Spring 6's `ProblemDetail`.
2. **Trace Correlation**: Every incoming HTTP request is assigned a `traceId` (extracted from `X-Correlation-Id` or newly generated), placed in Logback's `MDC`, injected into the response headers, and appended to the Problem Details object.
3. **Security (OAuth 2.0 Resource Server)**:
   - Endpoints are secured via JWT tokens.
   - Fine-grained scope authorization: `SCOPE_activations:write` for creation, `SCOPE_activations:read` for queries.
   - BOLA protection: In multi-tenant deployments, token claims (`customerId`) must match queried entities.

## Consequences
- Predictable, standardized contract for API consumers.
- Rapid root-cause diagnostics via correlation `traceId`.
- Zero internal exception leaks.
