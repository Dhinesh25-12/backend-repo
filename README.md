# Insurance Policy Management Portal — Backend

A Spring Boot backend for a cloud-native Insurance Policy Management Portal, covering
user management, insurance products, policies, claims, payments, and analytical reports.

## Tech Stack

- Java 17, Spring Boot 3.2 (Web, Security, Data JPA, Validation, Actuator)
- Spring Security + JWT (jjwt) for authentication/authorization
- Hibernate / Spring Data JPA
- PostgreSQL + Flyway migrations
- springdoc-openapi (Swagger UI)
- JUnit 5 / Spring Boot Test / H2 (test-only in-memory database)

## Project Layout

```
src/main/java/com/insurance/portal
├── controller   # REST controllers
├── service      # Business logic
├── repository   # Spring Data JPA repositories
├── entity       # JPA entities + enums
├── dto          # Request/response DTOs (request/response subpackages)
├── security     # JWT, UserDetails, filters
├── config       # Security & OpenAPI configuration
└── exception    # Global exception handling
src/main/resources/db/migration   # Flyway SQL migrations
```

## Setup

### 1. PostgreSQL

Create a database and user (adjust as needed):

```sql
CREATE DATABASE insurance_portal;
CREATE USER insurance_user WITH PASSWORD 'insurance_pass';
GRANT ALL PRIVILEGES ON DATABASE insurance_portal TO insurance_user;
```

### 2. Environment variables

| Variable            | Default                                              | Description               |
|---------------------|-------------------------------------------------------|----------------------------|
| `DB_URL`             | `jdbc:postgresql://localhost:5432/insurance_portal`    | JDBC URL                   |
| `DB_USERNAME`        | `insurance_user`                                       | DB username                |
| `DB_PASSWORD`        | `insurance_pass`                                       | DB password                |
| `JWT_SECRET`         | (dev default, **override in production**)              | HMAC secret for signing JWTs |
| `JWT_EXPIRATION_MS`  | `3600000` (1 hour)                                      | JWT expiry in ms           |
| `SERVER_PORT`        | `8080`                                                  | HTTP port                  |

### 3. Run

```bash
mvn spring-boot:run
```

Flyway automatically runs migrations (`src/main/resources/db/migration`) on startup,
creating the schema and seeding roles/users/products/sample domain data.

### 4. Build & Test

```bash
mvn clean verify   # compiles, runs unit/integration tests (H2 in-memory DB)
```

## Database Migrations

| Migration                         | Contents |
|------------------------------------|----------|
| `V1__core_schema.sql`               | Core tables: `role`, `app_user`, `user_role`, `customer`, `product`, `policy`, `claim`, `payment` + indexes |
| `V2__seed_data.sql`                 | Seed roles, sample users (admin/agent/claims officer/customers), products, and one sample policy/claim/payment |
| `V3__views_functions.sql`           | Reporting views (`policy_summary_view`, `claim_summary_view`, `payment_rollup_view`), PL/pgSQL functions (`fn_generate_policy_number`, `fn_claims_settlement_ratio`, `fn_top_customers_by_premium` using CTE + `RANK()` window function), and documented partitioning strategy for the `payment` table |

## Default Seeded Credentials

All seeded users share the password **`Password123!`**.

| Username    | Role            |
|-------------|-----------------|
| `admin`     | ADMIN           |
| `agent1`    | AGENT           |
| `claims1`   | CLAIMS_OFFICER  |
| `customer1` | CUSTOMER        |
| `customer2` | CUSTOMER        |

## API Documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Health check: `http://localhost:8080/actuator/health`

## Role Matrix (summary)

| Endpoint group                          | CUSTOMER | AGENT | CLAIMS_OFFICER | ADMIN |
|------------------------------------------|:--------:|:-----:|:---------------:|:-----:|
| `/api/auth/**`                            | public   | public| public           | public|
| `/api/products` GET                       | ✔        | ✔     | ✔                | ✔     |
| `/api/products` POST/PUT/DELETE           |          |       |                  | ✔     |
| `/api/policies/purchase`, `/renew`, `/cancel` | ✔    |       |                  |       |
| `/api/policies/{id}/cancel/approve`       |          |       |                  | ✔     |
| `/api/policies` (list all), `/agent/{id}` |          | ✔     |                  | ✔     |
| `/api/claims` (file)                      | ✔        |       |                  |       |
| `/api/claims/{id}/decision`, `/queue`     |          |       | ✔                |       |
| `/api/payments` (pay/history)             | ✔        |       |                  |       |
| `/api/reports/customer/**`                | ✔        |       |                  |       |
| `/api/reports/admin/**`                   |          |       |                  | ✔     |
| `/api/users` (list/role/activate)         |          |       |                  | ✔     |

## Sample curl commands

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"Password123!"}'
```

**Purchase a policy (use the JWT token from login):**
```bash
curl -X POST http://localhost:8080/api/policies/purchase \
  -H "Authorization: ******" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"nomineeName":"Mary Doe","nomineeRelationship":"Spouse","nomineeContact":"+1-555-0199"}'
```

**File a claim:**
```bash
curl -X POST http://localhost:8080/api/claims \
  -H "Authorization: ******" \
  -H "Content-Type: application/json" \
  -d '{"policyId":1,"incidentDate":"2024-05-01","description":"Minor accident damage"}'
```

**Admin: top 10 customers by premium (CTE + window function report):**
```bash
curl -H "Authorization: ******" \
  "http://localhost:8080/api/reports/admin/top-customers?limit=10"
```

## Known Limitations

- Payment/claims document handling is metadata-only (`documentUrl` string); no real object storage integration.
- No real payment gateway; premium payments are recorded directly as successful.
- Receipt generation produces a plain-text file (placeholder for a PDF receipt).
- Admin analytical reports (`fn_top_customers_by_premium`, `payment_rollup_view`, PL/pgSQL functions) require a real PostgreSQL datasource; they are not exercised against the H2 test database used for automated tests.
- The `payment` table is created as a regular table (not physically partitioned) for portability across PostgreSQL and the H2 test database; the migration includes the documented PostgreSQL range-partitioning DDL for production use.
