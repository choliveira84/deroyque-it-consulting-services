# Feature Specification: OpenAPI and Swagger UI Documentation

**Feature Branch**: `002-openapi-swagger`  
**Created**: 2026-10-28  
**Status**: Draft  
**Input**: User description: "quero um novo specification para que possamos usar o openapi/swagger"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Interactive API Exploration via Swagger UI (Priority: P1)

As an API consumer, technical evaluator, or frontend developer,  
I want to navigate to an interactive web interface (Swagger UI) in my browser,  
So that I can visualize all activation endpoints, inspect their request/response schemas and sample JSON payloads, and execute requests directly against the running service.

**Why this priority**:
Providing interactive Swagger UI directly in the application makes the service discoverable, self-documenting, and dramatically simplifies manual testing, technical evaluation, and client integration without needing external tools like Postman or cURL.

**Independent Test**:
Start the application and navigate to `http://localhost:8080/swagger-ui.html` (or `http://localhost:8080/swagger-ui/index.html`). The Swagger UI dashboard renders correctly without authentication prompts, listing all activation API operations with full schemas and examples.

**Acceptance Scenarios**:
1. **Given** the service is running, **When** a user accesses `GET /swagger-ui.html` or `GET /swagger-ui/index.html`, **Then** the browser renders the Swagger UI interface displaying API title, description, version, and the `/api/v1/requests` operation tags.
2. **Given** Swagger UI is open, **When** the user expands `POST /api/v1/requests`, **Then** the request body schema shows fields `externalId`, `customerId`, `productCode`, `productType`, and `payload` with realistic example values.
3. **Given** Swagger UI is open, **When** the user inspects error responses, **Then** HTTP 400, 401, 404, and 409 schemas display the RFC 9457 `ProblemDetail` model with `type`, `title`, `status`, `detail`, `instance`, and `traceId`.

---

### User Story 2 - Automated OpenAPI Specification Endpoint (Priority: P1)

As a developer or CI/CD automated pipeline,  
I want to fetch the machine-readable OpenAPI 3.0 specification via HTTP in JSON and YAML formats,  
So that I can generate client SDKs, validate API contracts, or publish specs to internal developer portals.

**Why this priority**:
Machine-readable schemas (`/v3/api-docs`) are essential for contract-first workflows, automated API gateway sync, and client code generation.

**Independent Test**:
Execute `curl -s http://localhost:8080/v3/api-docs`. The response is HTTP 200 OK with `Content-Type: application/json` containing a valid OpenAPI 3.0.x or 3.1.x specification document.

**Acceptance Scenarios**:
1. **Given** the service is running, **When** an unauthenticated client requests `GET /v3/api-docs`, **Then** the server responds with 200 OK and a valid OpenAPI 3 document in JSON format.
2. **Given** the service is running, **When** an unauthenticated client requests `GET /v3/api-docs.yaml`, **Then** the server responds with 200 OK and a valid OpenAPI 3 document in YAML format.

---

### User Story 3 - OAuth 2.0 Bearer JWT Authentication in Swagger UI (Priority: P2)

As a developer testing secured endpoints through Swagger UI,  
I want to click the "Authorize" button and provide a Bearer JWT token,  
So that Swagger UI automatically attaches the `Authorization: Bearer <token>` header to all outgoing requests and allows successful testing of protected endpoints (`POST` and `GET /api/v1/requests`).

**Why this priority**:
Because activation endpoints are protected by OAuth 2.0 Resource Server scope checks (`activations:write` and `activations:read`), developers need a convenient way to authorize inside the UI; otherwise, "Try it out" calls would fail with HTTP 401 Unauthorized.

**Independent Test**:
In Swagger UI, click "Authorize", type `dev-token` (or a mock JWT), submit, and execute `POST /api/v1/requests`. The request succeeds with HTTP 201 Created instead of HTTP 401 Unauthorized.

**Acceptance Scenarios**:
1. **Given** Swagger UI is loaded, **When** the user views the header, **Then** an "Authorize" button is visible with the `bearerAuth` (HTTP Bearer JWT) scheme.
2. **Given** the user entered `dev-token` into the authorization modal, **When** the user triggers "Execute" on `GET /api/v1/requests`, **Then** the outgoing HTTP request includes `Authorization: Bearer dev-token` and returns 200 OK.
3. **Given** no token is supplied, **When** the user triggers "Execute" on `POST /api/v1/requests`, **Then** the server responds with 401 Unauthorized as expected.

---

### User Story 4 - Public Security Exemption for Documentation Endpoints (Priority: P1)

As a system administrator or reviewer,  
I want the Swagger UI and OpenAPI documentation endpoints to be accessible publicly without requiring OAuth credentials,  
While keeping all functional activation endpoints strictly protected by Spring Security.

**Why this priority**:
Developers and automated evaluators must be able to view the API documentation without needing an OAuth 2 token beforehand.

**Independent Test**:
Send an unauthenticated request to `http://localhost:8080/swagger-ui/index.html` and verify it returns HTTP 200 OK without redirecting to login or returning HTTP 401.

**Acceptance Scenarios**:
1. **Given** an unauthenticated client, **When** requesting `/swagger-ui/**`, `/v3/api-docs/**`, or `/swagger-ui.html`, **Then** the request is permitted without authentication (`permitAll`).
2. **Given** an unauthenticated client, **When** requesting `POST /api/v1/requests` or `GET /api/v1/requests/**`, **Then** the request is still denied with HTTP 401 Unauthorized.

---

## Edge Cases

- **Custom Context Path / Reverse Proxy**: Swagger UI should dynamically resolve server URLs and context paths accurately without hardcoded `localhost:8080`.
- **Spring Boot Actuator Endpoints**: Actuator endpoints (`/actuator/**`) should not clutter the primary business API documentation unless explicitly tagged or separated in an actuator group.
- **Problem Details Response Deserialization**: RFC 9457 `ProblemDetail` fields must be properly mapped with descriptions for evaluation of error cases (400, 404, 409).
- **Correlation ID Header**: Responses should document `X-Correlation-Id` as an HTTP header present in API responses for distributed tracing.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST integrate `springdoc-openapi-starter-webmvc-ui` (SpringDoc OpenAPI 2.x for Spring Boot 3.3.x).
- **FR-002**: System MUST serve interactive Swagger UI at `/swagger-ui.html` and `/swagger-ui/index.html`.
- **FR-003**: System MUST serve OpenAPI 3.0 specification at `/v3/api-docs` (JSON) and `/v3/api-docs.yaml` (YAML).
- **FR-004**: System MUST configure Spring Security to permit public access (`permitAll`) to documentation routes:
  - `/swagger-ui.html`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
- **FR-005**: System MUST document the HTTP Bearer Authentication scheme (`bearerAuth` / JWT) in the OpenAPI definition so clients can authenticate via Swagger UI.
- **FR-006**: System MUST document all operations of `ActivationController`:
  - `POST /api/v1/requests` (Create activation): documented with 201 Created (and `Location` header), 200 OK (idempotent replay), 400 Bad Request, 401 Unauthorized, 409 Conflict.
  - `GET /api/v1/requests/{id}` (Get by ID): documented with 200 OK, 401 Unauthorized, 404 Not Found.
  - `GET /api/v1/requests` (List with filters/pagination): documented with query parameters, 200 OK, 401 Unauthorized.
- **FR-007**: System MUST provide meaningful descriptions, summary texts, and realistic payload examples for DTOs (`CreateActivationRequestDto`, `ActivationResponseDto`, `PageResponseDto`).
- **FR-008**: System MUST preserve Hexagonal Architecture boundaries and ArchUnit rule enforcement: OpenAPI annotations must reside within the infrastructure/web presentation layer without leaking into domain or application core.

### Key Entities & Contracts

- **OpenAPI Configuration Bean**: Spring configuration bean (`OpenApiConfig`) defining API metadata (title: "Product Activation Service API", version: "v1.0", description, contact info) and security scheme (`bearerAuth`).
- **Swagger UI Asset Routes**: Web MVC paths `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`.
- **RFC 9457 ProblemDetail Schema**: Standardized error schema for client-facing exceptions.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Swagger UI is accessible at `http://localhost:8080/swagger-ui.html` in under 1 second without authentication.
- **SC-002**: OpenAPI spec is served at `http://localhost:8080/v3/api-docs` with 100% valid OpenAPI 3 syntax.
- **SC-003**: 100% of activation endpoints (`POST /api/v1/requests`, `GET /api/v1/requests/{id}`, `GET /api/v1/requests`) are documented with HTTP status codes and example schemas.
- **SC-004**: Swagger UI allows executing authenticated requests using the "Authorize" button and Bearer token.
- **SC-005**: All existing 23 unit, integration, and ArchUnit architecture tests continue to pass without regression.

---

## Assumptions

- SpringDoc OpenAPI starter (`org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`) is compatible with Spring Boot 3.3.5 and Spring Security 6.
- Swagger UI will be enabled across all environments (including production and docker), but can be customized via properties if needed.
- Existing business logic and domain classes will not be altered or contaminated with Swagger annotations, honoring hexagonal architecture rules.
