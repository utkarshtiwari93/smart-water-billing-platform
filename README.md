# Smart Water Usage Monitoring & Automated Billing Platform

A backend platform for apartment communities that tracks household water consumption, and (in upcoming milestones) turns it into fair tiered bills, leak alerts and PDF invoices.

**Status:** Milestone 1 (Weeks 1-2, foundation) is complete. Milestones 2-4 are planned (see [Roadmap](#roadmap)).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 17+, Spring Boot 4.1 |
| Security | Spring Security 6+, JWT (jjwt 0.12), BCrypt |
| Database | MySQL 8 |
| ORM / Migrations | Spring Data JPA (Hibernate), Flyway |
| CSV parsing | Apache Commons CSV |
| Utilities | Lombok, Jakarta Bean Validation |
| Testing | JUnit 5, Mockito, MockMvc, Spring Security Test |

---

## Features (Milestone 1)

- **Database design:** 6 tables (`apartments`, `households`, `users`, `water_usage_logs`, `tariff_plans`, `billing_cycles`) with foreign keys, a unique constraint that blocks duplicate readings, and versioned Flyway migrations.
- **Authentication & roles:** JWT login, BCrypt password hashing, `ADMIN` and `RESIDENT` roles with method-level authorization.
- **Onboarding:** CRUD for apartments and households (including water meter number), resident registration, assigning residents to households, profile update and password change.
- **Usage logging:**
  - Manual meter reading entry
  - Bulk CSV upload with row-level validation and an error report
  - Consumption is calculated automatically: `current reading - previous reading` (first reading is the baseline)
  - Residents can see only their own household's usage
- **Validation & errors:** Jakarta Bean Validation, plus a global exception handler returning clean JSON errors (`400`, `401`, `403`, `404`, `409`).
- **Tests:** 53 automated tests (Mockito service tests, repository tests, MockMvc API tests).

---

## Getting Started

### Prerequisites

- JDK 17 or newer
- Maven 3.9+ (or use the included `mvnw`)
- MySQL 8 running locally

### 1. Clone

```bash
git clone <your-repo-url>
cd Water-Usage-Monitoring
```

### 2. Configure

The app reads its settings from `src/main/resources/application.yml`. The database `waterdb` is created automatically on first start. Provide your own credentials and secrets through environment variables instead of committing them:

| Variable | Purpose | Notes |
|---|---|---|
| `SPRING_DATASOURCE_USERNAME` | MySQL user | Overrides `spring.datasource.username` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password | Overrides `spring.datasource.password` |
| `JWT_SECRET` | Key used to sign tokens | At least 32 characters. A dev-only default exists, **change it outside local development** |
| `ADMIN_PASSWORD` | Password of the seeded admin | Default is for local development only |

Example (PowerShell):

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "your-mysql-password"
$env:JWT_SECRET = "a-long-random-secret-at-least-32-characters"
$env:ADMIN_PASSWORD = "choose-a-strong-password"
```

### 3. Run

```bash
./mvnw spring-boot:run
```

On startup Flyway creates the schema, and an admin user is seeded if no admin exists:

- Email: `admin@water.local` (from `app.admin.email`)
- Password: value of `ADMIN_PASSWORD` (or the default in `application.yml`)

The API is available at `http://localhost:8080`.

### 4. Run the tests

```bash
./mvnw test
```

Integration tests use a separate database, `waterdb_test`, which is created automatically, so your development data is never touched.

---

## Authentication

1. `POST /api/v1/auth/login` returns a JWT.
2. Send it on every protected request: `Authorization: Bearer <token>`.

| Status | Meaning |
|---|---|
| `401` | Missing, invalid or expired token, or wrong login credentials |
| `403` | Authenticated, but the role is not allowed |

Public registration always creates a `RESIDENT`. The first `ADMIN` is seeded at startup.

---

## API Reference

Base path: `/api/v1`

### Auth

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register a resident (`name`, `email`, `password`, `apartmentId`, optional `householdId`) |
| POST | `/auth/login` | Public | Login, returns JWT |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/users/me` | Any logged-in user | Own profile |
| PUT | `/users/me` | Any logged-in user | Update own name |
| PUT | `/users/me/password` | Any logged-in user | Change password |
| GET | `/users?apartmentId=` | ADMIN | List users (optional apartment filter) |
| PUT | `/users/{userId}/household` | ADMIN | Assign a resident to a household |
| DELETE | `/users/{userId}/household` | ADMIN | Remove a resident from a household |

### Apartments & Households (ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/apartments` | Create apartment |
| GET | `/apartments` | List apartments |
| GET / PUT / DELETE | `/apartments/{id}` | Read, update, delete apartment |
| POST | `/apartments/{apartmentId}/households` | Add household (flat, area, occupancy, meter number) |
| GET | `/apartments/{apartmentId}/households` | List households of an apartment |
| GET / PUT / DELETE | `/households/{id}` | Read, update, delete household |

### Water Usage

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/usage` | ADMIN | Add a manual reading (`householdId`, `readingDate`, `meterReading`) |
| POST | `/usage/upload` | ADMIN | Bulk CSV upload (`multipart/form-data`, field name `file`) |
| GET | `/usage?householdId=&from=&to=` | ADMIN, or the resident of that household | Readings with consumption (`from`/`to` are `yyyy-MM-dd`, default is the last 30 days) |

**Usage rules**

- The first reading of a household is a baseline (consumption `0`).
- A new reading must have a later date than the latest reading and must not be lower than it.
- One reading per household per date.
- Future dates are rejected.
- Meter readings are treated as litres.

### CSV format

```csv
meterNumber,readingDate,meterReading
MTR-001,2026-10-01,1000
MTR-001,2026-10-02,1015
```

Valid rows are imported and invalid rows are reported, so one bad row never blocks the rest:

```json
{
  "totalRows": 8,
  "imported": 3,
  "failed": 5,
  "errors": [
    { "rowNumber": 4, "message": "Reading already exists for 2026-10-03" },
    { "rowNumber": 6, "message": "No household found for meter MTR-XXX" }
  ]
}
```

Limits: `.csv` files only, up to 5 MB and 10,000 rows.

### Error format

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-10-16T10:15:30",
  "fieldErrors": { "name": "must not be blank" }
}
```

---

## Project Structure

```
src/main/java/com/infosys/Water_Usage_Monitoring
├── config        Security configuration, admin seeder
├── controller    REST controllers
├── dto           Request / response records
├── entity        JPA entities
├── enums         Role, ReadingSource, CycleStatus
├── exception     Custom exceptions + global handler
├── repository    Spring Data JPA repositories
├── security      JWT service and authentication filter
└── service       Business logic

src/main/resources
├── application.yml
└── db/migration  Flyway scripts (V1__init.sql)

src/test/java     Service, repository and API tests
```

---

## Database

Schema is managed only by Flyway; Hibernate runs with `ddl-auto: validate`.

- Never edit a migration that has already been applied or shared. Add a new `V2__...sql` instead.
- On a local development database, a checksum error after editing `V1` can be fixed by dropping and recreating `waterdb`.

---

## Roadmap

| Milestone | Weeks | Scope | Status |
|---|---|---|---|
| 1 | 1-2 | Schema, security, onboarding, usage logging, CSV upload, tests | Done |
| 2 | 3-4 | Tiered billing engine, bulk purchase module, cost distribution, alert scheduler, billing cycle management | Planned |
| 3 | 5-6 | React dashboards, PDF invoices, peer benchmarking, email notifications | Planned |
| 4 | 7-8 | Integration and load testing, UI polish, Swagger docs, Docker Compose, ER diagrams, final demo | Planned |

---

## Team

- Utkarsh Tiwari
- _Add teammate names_
