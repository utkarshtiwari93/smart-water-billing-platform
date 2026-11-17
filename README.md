# Smart Water Usage Monitoring & Automated Billing Platform

A full-stack platform for apartment communities to track water use, manage progressive tariffs and billing cycles, distribute shared costs, compare consumption, and deliver invoices and alerts.

**Status:** Core scope is implemented. See [Features](#implemented-features) and [Known boundaries](#known-boundaries) for current behavior.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot 4.1 |
| Security | Spring Security, JWT (jjwt 0.12), BCrypt, Google OAuth |
| Database | MySQL 8 |
| ORM / Migrations | Spring Data JPA (Hibernate), Flyway |
| CSV parsing | Apache Commons CSV |
| Charts / PDF / Email | React, Recharts, Apache PDFBox, Spring Mail |
| Utilities | Lombok, Jakarta Bean Validation |
| Testing | JUnit 5, Mockito, Spring Boot integration tests |

---

## Implemented Features

- **Apartment setup and access control:** Apartment and household management, meter configuration, email-verified resident registration, email-code password resets, JWT authentication, Google login, and role-based apartment/household data restrictions.
- **Water usage:** Manual meter entry and CSV import with row-level validation, duplicate detection, and error reporting. Daily and monthly trends allocate intervals across dates/months rather than assigning the full interval to its final reading date.
- **Resident experience:** Responsive React dashboard with daily/monthly Recharts trends, cycle usage and estimates where available, previous-period comparison, invoice history and PDF downloads, water-saving feed, and private peer benchmarking.
- **Admin experience:** Household usage comparisons and apartment average, meter CSV import, billing-cycle controls, progressive tariff configuration, bulk water purchase entry, and shared-cost previews.
- **Billing:** Billing cycles can be opened, finalized, and archived. Finalization produces persisted, itemized household invoices using the cycle's tariff, consumption, shared-cost allocation, and adjustments.
- **Progressive tariffs:** Each plan supports contiguous usage slabs with nondecreasing rates and an unbounded final slab. Existing two-tier plans are migrated as equivalent two-slab plans.
- **PDF invoices:** The backend generates PDFBox invoices containing apartment and household information, bill period, meter readings, consumption, persisted itemized charges, total payable, and payment instructions.
- **Alerts and email:** In-app and HTML email notifications for overuse/leak anomalies, bill-ready emails, and a separate water-saving tips email sent with finalized monthly bills. SMTP errors are logged and do not interrupt billing.
- **Peer benchmarking:** Residents see their household against apartment and comparable-size averages where data permits; other households' names and identities are not disclosed.
- **AquaBot:** An optional in-app assistant with resident and admin context, using the configured Gemini or Ollama provider when enabled. It can be disabled or run without an external model provider.
- **Validation and testing:** Jakarta Bean Validation and service-level guards protect API input and business rules. Unit and integration tests cover billing, access restrictions, usage, tariffs, alerts, and invoices.

---

## Getting Started

### Prerequisites

- JDK 21 or newer
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
| `DB_HOST`, `DB_PORT`, `DB_NAME` | MySQL endpoint/database | Defaults to `localhost:3306/waterdb` |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL credentials | Configure for your local database |
| `JWT_SECRET` | Key used to sign tokens | Required; use a random secret of at least 32 characters |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Seeded admin credentials | Configure a strong password |
| `SMTP_HOST`, `SMTP_PORT` | SMTP server | Defaults to Gmail (`smtp.gmail.com:587`) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | SMTP credentials | Use your Gmail address + a [Gmail App Password](https://myaccount.google.com/apppasswords) (not your normal password) |
| `MAIL_FROM` | Sender address | Leave blank to use `SMTP_USERNAME`; for Gmail it must match that address |
| `MAIL_CONSOLE_CODES` | Local fallback | Defaults to `true`; prints verification codes in the **backend** console when SMTP is unset |
| `ALERT_SCHEDULER_ENABLED` | Enable scheduled alert scans | Defaults to `false` |
| `AQUABOT_ENABLED`, `AQUABOT_PROVIDER` | Configure AquaBot | Provider can be `gemini`, `ollama`, or `none` |
| `GEMINI_API_KEY`, `AQUABOT_GEMINI_MODEL` | Gemini provider | Required only when Gemini is enabled |
| `OLLAMA_BASE_URL`, `AQUABOT_OLLAMA_MODEL` | Ollama provider | Required only when Ollama is enabled |

Example (PowerShell):

```powershell
$env:DB_USERNAME = "your-mysql-user"
$env:DB_PASSWORD = "your-mysql-password"
$env:JWT_SECRET = "a-long-random-secret-at-least-32-characters"
$env:ADMIN_PASSWORD = "choose-a-strong-password"

# Gmail delivery (required for real inbox mail):
$env:SMTP_USERNAME = "your-gmail@gmail.com"
$env:SMTP_PASSWORD = "your-16-char-app-password"
$env:MAIL_FROM = "your-gmail@gmail.com"
```

Without `SMTP_USERNAME` / `SMTP_PASSWORD`, the backend still works locally and prints the 6-digit code in the **Spring Boot console** (not the browser console).

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
| POST | `/auth/register/send-code` | Public | Send a six-digit email verification code (`email`); codes expire after 10 minutes and can be resent after 60 seconds |
| POST | `/auth/register` | Public | Register a resident (`name`, `email`, `password`, `apartmentId`, optional `householdId`, `verificationCode`) |
| POST | `/auth/login` | Public | Login, returns JWT |
| POST | `/auth/password/forgot` | Public | Request a six-digit password reset code (`email`); response does not reveal whether the account exists |
| POST | `/auth/password/verify-code` | Public | Verify reset code (`email`, `code`); returns a short-lived reset token |
| POST | `/auth/password/reset` | Public | Set a new password (`email`, `resetToken`, `newPassword`) |

Email verification applies to password-based resident registration. Google sign-in is unchanged. Verification and reset codes expire after 10 minutes; verification attempts are limited. Both `/auth/register/send-code` and `/auth/password/forgot` return a generic success message so responses do not reveal whether an email is already registered. For Gmail delivery, use an App Password and set `SMTP_USERNAME` / `SMTP_PASSWORD` / `MAIL_FROM` before starting the backend. If SMTP is unset, the 6-digit code is printed in the Spring Boot console instead.

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

### Billing, purchases, tariffs, and invoices

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST / GET | `/apartments/{apartmentId}/billing-cycles` | Apartment ADMIN | Create/list cycles |
| GET / PUT / DELETE | `/billing-cycles/{id}` | Apartment ADMIN | View/update/delete an open cycle |
| GET | `/billing-cycles/{cycleId}/distribution` | Apartment ADMIN | Preview water and shared-cost allocation |
| POST / GET | `/billing-cycles/{cycleId}/purchases` | Apartment ADMIN | Record/list bulk water purchases |
| POST | `/billing-cycles/{cycleId}/finalize` | Apartment ADMIN | Persist invoices and finalize the cycle |
| POST | `/billing-cycles/{cycleId}/archive` | Apartment ADMIN | Archive a finalized cycle |
| POST / GET | `/apartments/{apartmentId}/tariffs` | Apartment ADMIN | Create/list progressive tariff plans |
| PUT / PATCH / DELETE | `/tariffs/{id}` or `/tariffs/{id}/activation` | Apartment ADMIN | Update, activate/deactivate, or delete an unused plan |
| GET | `/invoices/my` | Resident | List the authenticated resident's household invoices |
| GET | `/invoices/{id}/pdf` | Owner resident or authorized ADMIN | Download a PDF invoice |

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

- Migrations are append-only. The current schema includes apartments, households, users, meter logs, progressive tariff plans/slabs, billing cycles, purchases, invoices/items, adjustments, alert settings, and notifications.
- The database is MySQL, not PostgreSQL; this keeps the existing project database technology.
- `V6__add_progressive_tariff_slabs.sql` creates the slab table and converts existing two-tier plan values into equivalent rows without changing saved invoice amounts.
- Hibernate runs with `ddl-auto: validate`; do not edit an applied migration.

---

## How Billing and Notifications Work

### Consumption aggregation

Each meter log records consumption since the preceding reading. The backend treats that consumption as an interval and distributes it across the interval's calendar days proportionally, so month boundaries are split rather than attributed wholly to the newest reading date. Dashboard chart series are based on these backend aggregates. Missing readings remain missing; cycle estimates are labeled as estimates and are shown only when there is enough data.

### Progressive tariffs

Tariff slabs are cumulative thresholds in kilolitres. The first slab begins at 0 kL, each finite upper limit ends one slab and starts the next, and only the final slab is unbounded. For consumption `C`, each slab is charged only for `min(C, slab upper limit) - slab lower limit`, clamped to zero, multiplied by that slab's per-kL rate. Rates may stay the same or increase, not decrease. Slab charges are rounded to two decimal places and persisted as invoice items at finalization.

Tariff changes that affect a plan used by a finalized or archived cycle are rejected. Invoice amounts and line items are persisted; later tariff changes do not recalculate finalized bills.

### Shared water costs

The distribution preview totals recorded bulk-purchase costs and computes metered household water charges from the cycle tariff. When procurement cost exceeds those water charges, the difference is the shared-cost pool and is apportioned by household consumption. Households without meters receive estimated weights based on floor area and measured apartment consumption where possible; if there is no usable metered consumption, allocation falls back to area, then an equal split when no floor areas are available. If tariff water charges exceed procurement cost, the platform does not create a negative shared-cost pool. The allocation basis and amount are persisted in the household invoice.

### PDF invoices

PDFs are generated by the Spring Boot backend from the persisted invoice and invoice items. This keeps totals consistent with finalized billing, avoids trusting client-provided values, and allows the backend to enforce resident-household and admin-apartment access before returning the document.

### Peer benchmarking

Benchmarking excludes households without valid consumption from usage averages. It compares apartment totals and, where real household area data exists, similar-sized households. Household identities and resident information are never returned in peer-comparison results. Occupancy is not inferred when it is absent.

### Transactional email versus marketing email

Transactional emails are triggered by application events such as bill finalization or a usage alert and contain information needed to use the service. The tips message is a separate, general conservation email sent with a finalized monthly bill. It is not a bulk marketing campaign: there are no promotional lists, third-party campaign services, or marketing tracking. SMTP credentials remain environment-provided; TLS certificate checks must stay enabled.

## Running the Frontend

```powershell
Set-Location water-frontend
npm ci
npm run dev
```

For a production bundle, run `npm run build` in `water-frontend`.

## Known boundaries

- Progressive slabs support per-kL rates. Fixed monthly service fees are not modeled separately; applicable one-off fees can be represented as billing adjustments.
- Tips emails are sent alongside cycle-finalization bill notifications rather than on a separate independent schedule.
- Email delivery requires valid SMTP credentials and a certificate chain trusted by the Java runtime. Delivery failures are logged and do not roll back bills.

---

## Team

- Utkarsh Tiwari
