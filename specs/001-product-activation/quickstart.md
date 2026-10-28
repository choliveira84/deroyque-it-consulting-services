# Quickstart: Customer Product Activation Service

**Feature Branch**: `001-product-activation`

## 1. Prerequisites
- **Java 21** JDK installed
- **Docker** and **Docker Compose** (for running containers and Testcontainers)
- **Maven 3.9+** (or use included `./mvnw`)

## 2. Fast Verification (Build & Test)
Run all unit, integration (Testcontainers PostgreSQL), provider contract (WireMock), and architectural rules (ArchUnit) in a single command:
```bash
./mvnw clean verify
```

## 3. Running Locally with Docker Compose
To launch the full environment (PostgreSQL + WireMock External Provider Mock + Application):
```bash
docker compose up -d --build
```

Health check endpoint:
```bash
curl http://localhost:8080/actuator/health
```

## 4. API Usage Examples

### 4.1 Obtain / Use JWT Token
Endpoints require Bearer tokens with appropriate scopes. In local/test profile, mock tokens or pre-configured test keys can be used:
```bash
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 4.2 Submit Activation Request
```bash
curl -X POST http://localhost:8080/api/v1/requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "EXT-10001",
    "customerId": "CUST-9901",
    "productCode": "INTERNET-500"
  }'
```

Response: `201 Created`
```json
{
  "id": "0b7f3c1e-5d2a-4c4e-9a51-2f8d7b1e6c90",
  "externalId": "EXT-10001",
  "customerId": "CUST-9901",
  "productCode": "INTERNET-500",
  "status": "PENDING",
  "createdAt": "2026-10-28T18:00:00Z",
  "updatedAt": "2026-10-28T18:00:00Z"
}
```

### 4.3 Query Activation Status
```bash
curl -X GET http://localhost:8080/api/v1/requests/0b7f3c1e-5d2a-4c4e-9a51-2f8d7b1e6c90 \
  -H "Authorization: Bearer $TOKEN"
```

Response: `200 OK`
```json
{
  "id": "0b7f3c1e-5d2a-4c4e-9a51-2f8d7b1e6c90",
  "externalId": "EXT-10001",
  "customerId": "CUST-9901",
  "productCode": "INTERNET-500",
  "status": "COMPLETED",
  "createdAt": "2026-10-28T18:00:00Z",
  "updatedAt": "2026-10-28T18:00:02Z"
}
```

### 4.4 List with Pagination & Filters
```bash
curl -X GET "http://localhost:8080/api/v1/requests?customerId=CUST-9901&status=COMPLETED&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```
