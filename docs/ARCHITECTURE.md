# TourVerse Architecture

This document describes the architecture currently implemented in the
repository as of July 29, 2026.

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
| Flyway-managed schema V1-V14                                     |
+------------------------------------------------------------------+
```

The backend is the system of record. Both clients now cover authenticated
profiles, destination discovery/details, categories, favorites, reviews, and
private trips; the backend still implements additional platform modules.

## Production topology

The provider-neutral deployment under `deploy/` places Caddy at the public
boundary for automatic HTTPS, routes `/api/*` to the Ktor container, and serves
the React single-page application through Nginx. PostgreSQL is reachable only
inside the Compose network. The API runs Flyway before accepting requests and
keeps development seed destinations disabled in production.

Android production builds require an HTTPS API URL and private release-signing
configuration. Development-oriented Android flavors retain explicit local HTTP
overrides. GitHub Actions independently verifies the backend, web, and Android
projects without receiving production credentials.

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

Destination write routes require `ADMIN`; destination reads remain public.

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

The Android client uses Navigation Compose, a shared session manager,
Keystore-encrypted refresh-token persistence, feature ViewModels/repositories,
and build flavors that inject the API URL into `BuildConfig`.

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

The web application uses React Router, a shared application shell, centralized
session/authenticated-fetch handling, protected routes, and feature pages.
Focused Vitest tests cover destination queries, API errors, bearer attachment,
single-flight refresh, and failed-session cleanup.

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
Ktor      localhost:8081
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

## Global catalogue and provenance

Normal web and Android discovery always reads approved PostgreSQL
`destinations`. Country filters come from `/api/destinations/countries`; clients
do not maintain separate country lists. `countryCode` uses ISO 3166-1 alpha-2
where confidently known while the existing `country` display field remains
backward compatible.

External discovery is an administrator-only ingestion boundary:

```text
Wikidata / disabled OpenTripMap
        -> normalized candidate + import batch
        -> duplicate and licence review
        -> explicit ADMIN approval
        -> destination + source reference (one transaction)
        -> public PostgreSQL catalogue
```

Candidates, batches, and provenance are separate from public destinations.
Provider credentials remain backend-only. Wikidata uses machine-readable SPARQL
with a bounded result limit, timeout, User-Agent, and throttling. OpenTripMap is
deliberately disabled pending a key and policy review. Google support is limited
to a future external Place ID/linking boundary; reviews, ratings, photos, and
large place payloads are not copied.

## Cache-aware aggregation and map presentation

Provider adapters normalize external facts before validation, duplicate/merge
rules, private review, and persistence in PostgreSQL. PostgreSQL remains the
stable catalogue/cache and TourVerse REST remains the only destination-data API
used by clients. Public requests never call providers.

Curated fields are editorially locked and outrank refresh data. External core
facts expire after 30 days and coordinates use a 90-day policy; curated and
development-seed rows do not expire through providers.

Google Maps is presentation with platform-restricted client keys. Google Places
is a backend-only bounded ADMIN match service that persists only a Place ID and
minimal link/audit metadata. Raw Google payloads are not stored.
