# Research & Technical Decisions: OpenAPI and Swagger UI

**Feature**: `002-openapi-swagger`  
**Date**: 2026-10-28  
**Status**: Completed  

---

## 1. Library Selection: SpringDoc OpenAPI 2.x

### Context
Spring Boot 3.3.x requires an OpenAPI 3.0 / 3.1 generator that supports Jakarta EE 10 (`jakarta.servlet`, `jakarta.validation`) and Spring MVC 6.x.

### Alternatives Considered
1. **Springfox**: Deprecated, archived, and entirely incompatible with Spring Boot 3.x and Jakarta EE namespaces.
2. **SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui:2.6.0`)**: The official, de-facto standard implementation for Spring Boot 3. Provides automated OpenAPI 3 spec generation and embeds the latest Swagger UI assets.

### Decision
Adopt `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`.

---

## 2. Security Integration & Bearer JWT Scheme

### Context
All activation endpoints (`POST /api/v1/requests`, `GET /api/v1/requests/**`) require OAuth 2.0 Bearer tokens with specific scopes (`activations:write` and `activations:read`). Without configuring Swagger's security scheme, interactive testing via Swagger UI would fail with HTTP 401 Unauthorized.

### Technical Decision
1. Define a global OpenAPI `SecurityScheme` in `OpenApiConfig`:
   ```java
   new SecurityScheme()
       .name("bearerAuth")
       .type(SecurityScheme.Type.HTTP)
       .scheme("bearer")
       .bearerFormat("JWT")
       .description("Provide OAuth 2.0 Bearer JWT (or 'dev-token' under dev profile)");
   ```
2. Apply `security = @SecurityRequirement(name = "bearerAuth")` to the API operations or globally via `OpenAPI.addSecurityItem(...)`.
3. In `SecurityConfig.java`, add explicit `permitAll()` matchers for:
   - `/swagger-ui/**`
   - `/swagger-ui.html`
   - `/v3/api-docs/**`

---

## 3. Hexagonal Architecture Compliance & ArchUnit Verification

### Context
Constitution Principle I strictly dictates that domain and application layers must not depend on web or documentation libraries.

### Technical Decision
- All OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`) are confined strictly to:
  - `com.deroyque.activation.infrastructure.web` (Controllers, DTO records)
  - `com.deroyque.activation.infrastructure.configuration` (`OpenApiConfig`)
- Zero annotations or imports from `io.swagger.v3.*` or `org.springdoc.*` are permitted in `com.deroyque.activation.domain.*` or `com.deroyque.activation.application.*`.
- Existing ArchUnit tests (`ArchitectureRulesTest`) will enforce that domain remains independent of infrastructure.

---

## 4. RFC 9457 Problem Details Schema Mapping

### Context
The application uses standard `org.springframework.http.ProblemDetail` for all error responses (400 Bad Request, 404 Not Found, 409 Conflict).

### Technical Decision
Document Problem Details schema with explicit fields:
- `type` (URI reference identifying problem type)
- `title` (Human-readable summary)
- `status` (HTTP status code)
- `detail` (Explanation specific to this occurrence)
- `instance` (URI reference of the request)
- `traceId` (MDC correlation tracking ID)
- `invalidParams` (Array of field validation errors when status is 400)
