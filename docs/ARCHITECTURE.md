# TourVerse Architecture

This document describes the architecture currently implemented in the
repository as of July 28, 2026.

## System overview

```text
+-------------------------+       +-------------------------+
| Android application     |       | Web application         |
| Kotlin / Compose        |       | React / TypeScript      |
| Ktor Client / OkHttp    |       | Browser fetch           |
+------------+------------+       +------------+------------+
             | HTTP/JSON                           |
             +----------------+--------------------+
                              |
                              v
+------------------------------------------------------------------+
| Ktor backend                                                     |
|                                                                  |
| Plugins -> Routes -> Services -> Repositories/Exposed tables      |
|                                                                  |
| Auth | Profiles | Destinations | Categories | Community          |
| Services | Bookings | Notifications | Administration              |
+--------------------------------+---------------------------------+
                                 |
                                 | JDBC through HikariCP
                                 v
+------------------------------------------------------------------+
| PostgreSQL                                                       |
| Flyway-managed schema V1-V12                                     |
+------------------------------------------------------------------+
```

The backend is the system of record. Both clients are currently read-oriented
destination prototypes, while the backend implements the larger platform.

## Backend startup and request flow

```text
EngineMain
   |
   v
Application.module()
   |
   +--> load .env / process environment
   +--> create Hikari pool
   +--> validate and run Flyway migrations
   +--> connect Exposed
   `--> configure Ktor
          |
          +--> observability and security headers
          +--> serialization and status handling
          +--> rate limiting and CORS
          `--> routing
```

A normal request follows:

```text
HTTP request
   |
   v
Ktor plugins
   |
   v
Route handler
   |
   +--> bearer-token and role check when required
   +--> request deserialization
   v
Service validation and ownership rules
   |
   v
Repository or Exposed table query
   |
   v
PostgreSQL transaction
   |
   v
Serializable JSON response
```

Destination and category persistence use repository interfaces. Authentication,
profiles, community features, and platform features currently query Exposed
tables from their services directly.

## Backend modules

| Module | Main responsibility |
| --- | --- |
| Authentication | Registration, login, access tokens, refresh rotation, logout |
| Profiles | Basic and extended current-user profile management |
| Destinations | CRUD, search, filters, pagination, sorting |
| Categories | Public reads and administrator management |
| Reviews | Rating summaries and owner/admin moderation |
| Favorites | Per-user saved destinations |
| Trips | Private trips and ordered destination entries |
| Tourism services | Public discovery and owner/admin management |
| Bookings | Creation, cancellation, listing, admin status updates |
| Notifications | In-app booking notifications and read state |
| Administration | Statistics, users, roles, services, and bookings |
| Platform concerns | Errors, logging, CORS, headers, compression, rate limiting |

## Data architecture

Flyway owns the schema history:

```text
users 1----* refresh_tokens
  |
  +----* reviews *----1 destinations
  +----* favorites *--1 destinations
  +----* trips 1------* trip_destinations *----1 destinations
  +----* tourism_services *--------------------0..1 destinations
  +----* bookings *----1 tourism_services
  `----* notifications

categories are currently independent lookup records.
destinations.category is text rather than a category foreign key.
```

Deletes cascade for user-owned refresh tokens, reviews, favorites, trips,
bookings, and notifications. A destination deletion cascades reviews,
favorites, and trip entries. Bookings restrict deletion of their service; the
service layer deactivates a booked service instead.

## Authentication and trust boundaries

Access tokens are signed HMAC-SHA256 payloads containing user ID, role, issued
time, and expiry. They are bearer credentials and are verified on protected
routes. Refresh tokens are random opaque values stored only as SHA-256 hashes.
Passwords use salted PBKDF2-HMAC-SHA256 with 120,000 iterations.

Authorization is enforced in route helpers and service ownership checks:

- Categories and administration require `ADMIN`.
- Reviews are writable by their owner or an administrator.
- Trips and favorites are scoped to the authenticated user.
- Tourism services are created by administrators, business owners, or tour
  guides and managed by owners or administrators.
- Booking cancellation is restricted to the booking owner.

Destination write routes currently have no bearer or role check and should be
treated as an unfinished authorization boundary.

## Android architecture

```text
Compose HomeScreen
      ^
      | StateFlow<HomeUiState>
      |
HomeViewModel
      |
DestinationRepository
      |
TourismApi -> shared Ktor HttpClient -> backend
```

The Android client has one screen and no navigation or persistence layer.
Build flavors inject the API URL into `BuildConfig`.

## Web architecture

```text
React App
   |
   +--> local loading/error/data state
   |
   +--> getDestinations() -> fetch -> backend
   |
   `--> DestinationCard grid
```

The web application is a single Vite entry point with no router, global state,
cache, or authentication layer. Focused Vitest tests cover destination query
and API error behavior.

## Destination integration contract

Both clients now implement:

```text
UUID ID + country/city + coverImageUrl + timestamps
+ paginated { items, page, size, totalItems, totalPages }
```

Android uses Ktor query parameters and a paged DTO; web uses
`URLSearchParams` and abortable fetch. Both derive location from nullable city
and country, retain pagination metadata, handle missing cover URLs, and parse
the standard backend error response. Independent builds and unit tests do not
prove browser- or device-observed runtime behavior.

## Deployment topology

For local development:

```text
PostgreSQL localhost:5432
Ktor      localhost:8080
Vite      localhost:5173
Android   adb reverse, 10.0.2.2, or LAN host address
```

The backend Docker Compose file defines PostgreSQL and API services with health
checks and a named database volume. A production environment still requires
HTTPS termination, secret management, backups, monitoring, log collection,
safe administrator provisioning, and explicit frontend API URLs.

## External boundaries

The repository does not implement payment-provider execution, binary media
uploads, email delivery, push delivery, maps-provider services, or cloud
hosting. Database fields such as payment status and image URLs represent
application state, not proof of an external integration.
