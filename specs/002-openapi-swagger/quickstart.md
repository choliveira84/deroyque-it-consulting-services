# Quickstart: OpenAPI & Swagger UI Documentation

**Feature**: `002-openapi-swagger`  
**Date**: 2026-10-28  

---

## 1. Accessing Swagger UI

Once the application is started:

1. Open your browser and navigate to:
   ```text
   http://localhost:8080/swagger-ui.html
   ```
   (or `http://localhost:8080/swagger-ui/index.html`)

2. You will see the interactive dashboard:
   - **Service Title**: Deroyque Product Activation Service API
   - **Operations**:
     - `POST /api/v1/requests`: Create or replay an activation request
     - `GET /api/v1/requests/{id}`: Retrieve an activation request by ID
     - `GET /api/v1/requests`: List activation requests with filtering and pagination

---

## 2. Authenticating in Swagger UI

Because API endpoints require OAuth 2.0 Bearer JWT authorization:

1. Click the green **Authorize** button with the lock icon at the top right of Swagger UI.
2. Under `bearerAuth`, enter the token:
   ```text
   dev-token
   ```
   *(or a valid JWT in production/staging environments)*
3. Click **Authorize**, then **Close**.
4. Swagger UI will automatically attach the `Authorization: Bearer ...` header to all outgoing requests.

---

## 3. Fetching OpenAPI Specification Endpoints

### JSON Format
```bash
curl -s http://localhost:8080/v3/api-docs | jq .
```

### YAML Format
```bash
curl -s http://localhost:8080/v3/api-docs.yaml
```

---

## 4. Verification with Security Tests

Documentation endpoints are publicly accessible (`permitAll`):
```bash
# Returns HTTP 200 OK without requiring authentication
curl -i http://localhost:8080/v3/api-docs
curl -i http://localhost:8080/swagger-ui/index.html
```

Protected endpoints remain strictly secured:
```bash
# Returns HTTP 401 Unauthorized without Authorization header
curl -i -X POST http://localhost:8080/api/v1/requests
```
