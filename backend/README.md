# TourVerse Backend

This is the authoritative guide to the TourVerse Kotlin/Ktor backend. It
describes the implementation currently present in `backend`; planned external
integrations are identified separately.

## What is implemented

The backend provides a JSON REST API backed by PostgreSQL:

- Health and API-documentation endpoints
- Destination CRUD, search, filters, pagination, and sorting
- Registration, login, access tokens, refresh-token rotation, logout, and
  logout from all sessions
- Current-user account and extended profile management
- Administrator-managed tourism categories
- Destination reviews and rating summaries
- User favorites
- Private trips and ordered trip destinations
- Tourism-service listings and owner/administrator management
- Bookings, cancellation, and administrator status changes
- In-app booking notifications
- Administrator statistics, user roles, services, and bookings
- Centralized error responses, validation, CORS, security headers, compression,
  request logging, forwarded-header support, and in-memory rate limiting
- Flyway migrations, Docker packaging, an OpenAPI resource, and automated tests

Destination reads are public. Destination create, update, and delete routes
require a valid bearer access token whose role is `ADMIN`.

## Technology

| Area | Implementation |
| --- | --- |
| Language | Kotlin 2.3.20 on Java 17 |
| Server | Ktor 3.1.2 with Netty |
| Serialization | Kotlin serialization |
| Database | PostgreSQL |
| SQL access | Exposed 1.3.1 JDBC |
| Pooling | HikariCP 7.0.2 |
| Migrations | Flyway 12.11.0 |
| Authentication | Signed access tokens and rotating opaque refresh tokens |
| Password storage | PBKDF2-HMAC-SHA256 |
| Logging | SLF4J and Logback |
| Tests | Kotlin test and Ktor test host |

## Source organization

```text
backend/
|-- src/main/kotlin/com/tourverse/
|   |-- database/       PostgreSQL bootstrap and Exposed table definitions
|   |-- dto/            Shared API message and pagination responses
|   |-- exceptions/     HTTP-oriented domain exceptions
|   |-- models/         Serializable request and response models
|   |-- plugins/        Ktor serialization, HTTP, security, logging, and errors
|   |-- repositories/   Destination and category persistence
|   |-- routes/         HTTP endpoint definitions
|   |-- security/       Bearer-token parsing, tokens, and password hashing
|   |-- services/       Validation and application logic
|   `-- utils/          Environment access and custom serializers
|-- src/main/resources/
|   |-- db/migration/   Flyway V1 through V12
|   |-- openapi/        Machine-readable API document
|   |-- application.conf
|   `-- logback.xml
|-- src/test/kotlin/    Route, validation, password, token, and service tests
|-- .env.example
|-- .env.production.example
|-- Dockerfile
|-- docker-compose.yml
`-- README.md
```

`Application.module()` initializes the database and runs migrations before
installing the Ktor plugins and routes. Tests can call `configureApplication()`
without starting a real database.

## Configuration

The backend reads process environment variables and a local `.env` file through
`dotenv-kotlin`. Copy the example and supply your own values:

```powershell
Copy-Item .env.example .env
```

Required database variables:

```dotenv
TOURVERSE_DATABASE_URL=jdbc:postgresql://localhost:5432/tourverse_db
TOURVERSE_DATABASE_USER=tourverse_user
TOURVERSE_DATABASE_PASSWORD=your-local-database-password
```

Runtime and security variables:

| Variable | Purpose |
| --- | --- |
| `TOURVERSE_JWT_SECRET` | Signs access tokens; use a unique secret |
| `TOURVERSE_ENV` | Set to `production` to enable strict startup checks |
| `TOURVERSE_ALLOWED_ORIGINS` | Comma-separated CORS origins |
| `TOURVERSE_RATE_LIMIT_PER_MINUTE` | Per-client limit; defaults to 120 |
| `PORT` | Overrides the default server port 8080 |

Development permits a local fallback JWT secret and, when no origins are
configured, allows any CORS host. Production requires a non-placeholder JWT
secret of at least 48 characters and explicit non-wildcard origins.

Do not commit `.env`, real credentials, tokens, or production URLs.

## Local database and startup

Create the PostgreSQL database and role so their credentials match `.env`.
Then run:

```powershell
.\gradlew.bat clean build --no-daemon
.\gradlew.bat run
```

Default URLs:

```text
API:      http://localhost:8080
Health:   http://localhost:8080/api/health
Docs:     http://localhost:8080/api/docs
OpenAPI:  http://localhost:8080/api/openapi.yaml
```

Flyway validates and applies migrations during startup. Migration files already
applied to a persistent database are immutable: introduce schema changes in the
next numbered migration. A checksum mismatch means the file no longer matches
the database history; do not blindly repair a shared database.

Current migrations:

| Version | Schema change |
| --- | --- |
| V1 | Baseline marker |
| V2 | Destinations and `pgcrypto` |
| V3 | Users and refresh tokens |
| V4 | Extended user-profile fields |
| V5 | Categories and six seed categories |
| V6 | Reviews |
| V7 | Favorites |
| V8 | Trips and trip destinations |
| V9 | Tourism services |
| V10 | Bookings |
| V11 | Notifications |
| V12 | Administration/query indexes |

## Authentication and authorization

Destination write authorization is enforced in the Ktor route layer with the
same `authenticatedUser("ADMIN")` mechanism used by other administrator
operations.

### Development catalogue

`scripts/seed-development-data.sql` explicitly adds four category records and
36 conservative Uganda development destinations. It is not a Flyway migration,
is never run automatically, and must never be run against production.

Run it manually with a PostgreSQL client connected to a disposable development
database. It is idempotent: category slugs use `ON CONFLICT`, while destinations
are inserted only when the same case-insensitive name and country are absent.

If the password formerly present in `.env.example` was ever used, rotate the
PostgreSQL role password manually. If it was pushed remotely, consider it
compromised. Production credentials belong in the hosting platform's secret
manager.

Protected requests use:

```http
Authorization: Bearer <access-token>
```

Registration produces an access token, rotating refresh token, and user
response. Access tokens contain the user ID, role, issued time, and expiry.
Refresh tokens are stored only as SHA-256 hashes and are revoked on rotation or
logout. Passwords are salted PBKDF2-HMAC-SHA256 hashes.

Roles:

- `USER`
- `ADMIN`
- `TOUR_GUIDE`
- `BUSINESS_OWNER`

Review owners may update or delete their own reviews; administrators may
moderate any review. Tourism-service owners and administrators may manage their
services. Category and administration routes require `ADMIN`.

## Endpoint reference

### System and documentation

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | Service health response |
| GET | `/api/docs` | Public | HTML documentation landing page |
| GET | `/api/openapi.yaml` | Public | OpenAPI resource |

### Authentication and users

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public |
| POST | `/api/auth/logout` | Public; refresh token in body |
| POST | `/api/auth/logout-all` | Authenticated |
| GET | `/api/users/me` | Authenticated |
| PUT | `/api/users/me` | Authenticated |
| GET | `/api/users/me/profile` | Authenticated |
| PUT | `/api/users/me/profile` | Authenticated |
| PUT | `/api/users/me/profile/image` | Authenticated |
| DELETE | `/api/users/me` | Authenticated; password confirmation |

### Destinations

| Method | Path | Current access |
| --- | --- | --- |
| GET | `/api/destinations` | Public |
| GET | `/api/destinations/{id}` | Public |
| POST | `/api/destinations` | Public |
| PUT | `/api/destinations/{id}` | Public |
| DELETE | `/api/destinations/{id}` | Public |

List parameters are `search`, `country`, `city`, `category`, `page`, `size`,
`sortBy`, and `sortDirection`. Pages start at 1; sizes range from 1 to 100.
Sort fields are `name`, `country`, `city`, `category`, `createdAt`, and
`updatedAt`; direction is `asc` or `desc`.

`CreateDestinationRequest` and `UpdateDestinationRequest` both require the full
`name`, `country`, `description`, and `category` fields. Optional fields are
`city`, `latitude`, `longitude`, and `coverImageUrl`. Coordinates must be
provided together.

The list response is a paginated object:

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Destination IDs are UUID strings.

### Categories

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/categories` | Public; active only |
| GET | `/api/categories/{id}` | Public |
| POST | `/api/categories` | Admin |
| PUT | `/api/categories/{id}` | Admin |
| DELETE | `/api/categories/{id}` | Admin |
| GET | `/api/admin/categories` | Admin; includes inactive |

Create requires `name`; `description`, `iconUrl`, and `active` are optional.
Update accepts the same fields as nullable partial changes but requires at least
one field. Slugs are generated from names.

### Reviews, favorites, and trips

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/destinations/{destinationId}/reviews` | Public |
| POST | `/api/destinations/{destinationId}/reviews` | Authenticated |
| PUT | `/api/reviews/{id}` | Owner or admin |
| DELETE | `/api/reviews/{id}` | Owner or admin |
| GET | `/api/favorites` | Authenticated |
| POST | `/api/favorites/{destinationId}` | Authenticated |
| DELETE | `/api/favorites/{destinationId}` | Authenticated |
| GET | `/api/trips` | Authenticated owner |
| GET | `/api/trips/{id}` | Authenticated owner |
| POST | `/api/trips` | Authenticated |
| PUT | `/api/trips/{id}` | Authenticated owner |
| DELETE | `/api/trips/{id}` | Authenticated owner |
| POST | `/api/trips/{id}/destinations` | Authenticated owner |
| DELETE | `/api/trips/{id}/destinations/{destinationId}` | Authenticated owner |

Ratings range from 1 to 5, with one review per user and destination. Favorites
and trip destinations are unique per user/trip and destination. Trip end dates
cannot precede start dates.

### Tourism services, bookings, and notifications

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/services` | Public |
| GET | `/api/services/{id}` | Public |
| POST | `/api/services` | Admin, business owner, or tour guide |
| PUT | `/api/services/{id}` | Owner or admin |
| DELETE | `/api/services/{id}` | Owner or admin |
| GET | `/api/bookings` | Authenticated; admins see all |
| POST | `/api/bookings` | Authenticated |
| PUT | `/api/bookings/{id}/cancel` | Booking owner |
| GET | `/api/notifications` | Authenticated |
| PUT | `/api/notifications/{id}/read` | Authenticated owner |
| PUT | `/api/notifications/read-all` | Authenticated |

Service filters are `type` and `destinationId`. Types are `HOTEL`,
`RESTAURANT`, `TOUR`, `TRANSPORT`, `GUIDE`, and `ACTIVITY`. Bookings require a
non-past date and 1 to 100 people. Creating, cancelling, or administratively
updating a booking creates an in-app notification.

Deleting a service with bookings deactivates it; otherwise it is removed.

### Administration

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/admin/statistics` | Admin |
| GET | `/api/admin/users` | Admin |
| PUT | `/api/admin/users/{id}/role` | Admin |
| GET | `/api/admin/services` | Admin |
| GET | `/api/admin/bookings` | Admin |
| PUT | `/api/admin/bookings/{id}/status` | Admin |

Booking statuses are `PENDING`, `CONFIRMED`, `CANCELLED`, and `COMPLETED`.

## Error behavior and validation

Central status handling maps malformed bodies, validation failures,
authentication failures, authorization failures, conflicts, missing records,
and unexpected errors to safe JSON messages. Internal exceptions are logged
without returning stack traces to clients.

Notable rules include:

- Destination text length and coordinate-range validation
- Valid HTTP(S) image and icon URLs
- Registration name, email, and password rules
- Profile length, nationality, interests, and visibility validation
- Review rating 1–5 and comment length up to 2,000
- Category names 2–80 characters and descriptions up to 500
- Service type, email, URL, nonnegative price, and three-letter currency checks
- Booking date, party size, ownership, and terminal-status checks

## Tests and verification

Run the complete backend verification:

```powershell
.\gradlew.bat clean build --no-daemon
```

The current test suite covers:

- Health and documentation routes
- Destination query and request validation
- Authentication, category, and profile validation
- Password hashing and verification
- Access-token signing, verification, tampering, and expiry
- Platform validation rules

Runtime verification additionally requires a reachable PostgreSQL instance with
matching credentials and valid Flyway history:

```powershell
.\gradlew.bat run
```

## Docker deployment

Copy `.env.production.example` to a local `.env`, replace every placeholder,
then run:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f api
```

Stop without deleting the database volume:

```bash
docker compose down
```

Only use `docker compose down -v` when intentionally destroying the stored
database.

Before deployment, verify migrations against both an empty database and a
backup of the target database, configure HTTPS at the hosting layer, create the
initial administrator through a controlled process, set explicit frontend API
URLs, configure backups and monitoring, and rotate any exposed secret.

## Known boundaries

The backend stores image URLs and payment status but does not upload image
binaries or collect payments. It does not integrate with transactional email,
push notifications, map billing, or a hosted deployment provider. These require
external accounts, credentials, provider-specific code, and operational setup.

The Android and web clients now consume this backend's paginated UUID-based
destination contract, including nullable destination fields, backend
timestamps, pagination metadata, search and filter parameters, sorting, and
standard `ApiMessage` error responses.
