# TourVerse Creation Journal

## Purpose of this journal

This journal explains how TourVerse grew from an initial tourism application
idea into the current full-stack project. It records what was built, why it was
built, the technologies used, what each technology means, and how the pieces
work together.

The early repository does not contain a dated diary for every individual
decision. The chronology below is therefore reconstructed from the current
source code, Flyway migration sequence, Git-managed project structure, tests,
and maintained documentation. It describes implemented behavior and avoids
claiming that unfinished work is complete.

TourVerse currently consists of:

- A Kotlin/Ktor REST API.
- A PostgreSQL database managed by Flyway.
- A native Android application built with Kotlin and Jetpack Compose.
- A web application built with React and TypeScript.
- Destination catalogue import, review, provenance, caching, and map support.
- Docker-based staging and production infrastructure.
- Automated backend, web, and Android verification through GitHub Actions.

The project is repository-ready for staging. A real public deployment still
requires a hosting account, domain, DNS configuration, production secrets,
curated production data, monitoring, verified backups, and final live testing.

---

## 1. The original application goal

TourVerse was created as a tourism platform where travellers can discover
destinations, organize trips, interact with tourism services, make bookings,
write reviews, receive notifications, and manage a personal travel profile.
The project also supports administrators and tourism business owners.

The application needed more than a single user interface. It required:

1. A central backend that applies business and security rules.
2. A persistent relational database.
3. An Android client for mobile users.
4. A browser-based web client.
5. Administrative workflows for managing platform data.
6. A controlled way to grow a global destination catalogue.
7. Testing and deployment foundations suitable for eventual production use.

The backend was designed as the system of record. Android and web consume its
REST API rather than implementing their own versions of the business rules or
contacting destination providers directly.

---

## 2. Establishing the repository structure

The repository was divided into independent application folders:

```text
TourVerse/
|-- backend/       Kotlin/Ktor API and Flyway migrations
|-- frontend/
|   |-- androidApp/   Native Android application
|   `-- webApp/       React browser application
|-- docs/          Cross-project documentation
|-- postman/       API specifications and request collection
|-- deploy/        HTTPS Docker deployment and operations runbook
`-- .github/       Continuous-integration workflows
```

This separation lets each application use its natural build tools while still
sharing one source repository and one API contract.

### Git

**What it is:** Git is a distributed version-control system.

**What TourVerse uses it for:** Git records changes to source code,
documentation, migrations, tests, and deployment configuration. It supports
reviewable history, branches, rollback to earlier revisions, and collaboration.

**Important rule in TourVerse:** Secrets, database passwords, API keys,
keystores, compiled output, dependency folders, and local environment files are
excluded through `.gitignore`.

### Markdown

**What it is:** Markdown is a lightweight plain-text documentation format.

**What TourVerse uses it for:** The root README, application READMEs, API
contract, architecture guide, deployment runbook, and this journal are written
in Markdown so they can be read in GitHub or any ordinary text editor.

---

## 3. Creating the backend foundation

The backend was built with Kotlin and Ktor. Its job is to accept HTTP requests,
validate input, authenticate users, enforce roles and ownership, execute
business rules, read and write PostgreSQL data, and return JSON responses.

### Kotlin

**What it is:** Kotlin is a statically typed programming language designed for
the Java Virtual Machine and other platforms.

**Why TourVerse uses it:** Kotlin provides null-safety, concise data classes,
coroutines for asynchronous work, strong type checking, and excellent Android
support. Using Kotlin in both the backend and Android client reduces the mental
distance between their models and conventions.

### Java Virtual Machine and JDK

**What they are:** The Java Virtual Machine executes JVM bytecode. The Java
Development Kit provides the compiler, runtime, cryptography, and other tools
required to build JVM applications.

**How TourVerse uses them:** The Ktor backend runs with Java 17. Android source
is configured for Java/Kotlin 21 language targets while the Android toolchain
produces code for supported Android devices.

### Ktor

**What it is:** Ktor is JetBrains' Kotlin framework for building asynchronous
servers and network clients.

**What TourVerse uses it for:** Ktor powers the REST API, routing, JSON content
negotiation, CORS, bearer authentication, request logging, compression,
forwarded headers, default security headers, centralized error handling, and
the embedded Netty server.

The request path follows this general structure:

```text
HTTP request
    |
    v
Ktor plugins
    |
    v
Route
    |
    v
Service and validation
    |
    v
Repository / Exposed
    |
    v
PostgreSQL
```

### Netty

**What it is:** Netty is an asynchronous network application framework.

**What TourVerse uses it for:** Ktor uses Netty as the backend HTTP engine. It
listens on the configured port—currently `8081` locally—and dispatches requests
into the Ktor application.

### Kotlin serialization and JSON

**What they are:** Kotlin serialization converts Kotlin objects to and from
structured formats. JSON is the text format exchanged by the REST API.

**What TourVerse uses them for:** Request data classes are decoded from JSON,
and response models are encoded back into JSON. Custom serializers handle UUID
and timestamp types consistently.

### Gradle and the Gradle Wrapper

**What they are:** Gradle is the build automation system. The wrapper pins and
downloads the version expected by each project.

**What TourVerse uses them for:** Gradle resolves dependencies, compiles
Kotlin, runs tests, packages the backend distribution, builds Android variants,
and creates Android App Bundles. The wrapper scripts allow reproducible builds
without requiring a manually installed matching Gradle version.

---

## 4. Designing PostgreSQL persistence

TourVerse uses a relational database because users, destinations, reviews,
trips, services, bookings, and notifications have clear relationships and need
transactional consistency.

### PostgreSQL

**What it is:** PostgreSQL is an open-source relational database management
system.

**What TourVerse uses it for:** PostgreSQL stores accounts, refresh sessions,
destinations, categories, reviews, favourites, trips, tourism services,
bookings, notifications, import candidates, provider references, cache
metadata, and field provenance.

Relational constraints help prevent invalid references and duplicate records.
Transactions ensure related operations either complete together or are rolled
back together.

### JDBC

**What it is:** Java Database Connectivity is the standard JVM interface for
communicating with relational databases.

**What TourVerse uses it for:** The PostgreSQL JDBC driver carries SQL traffic
between the backend and PostgreSQL.

### HikariCP

**What it is:** HikariCP is a high-performance JDBC connection pool.

**What TourVerse uses it for:** Instead of opening a new database connection for
every request, the backend reuses a controlled pool of connections. This
reduces connection overhead and protects the database from uncontrolled
connection growth.

### Exposed

**What it is:** Exposed is JetBrains' Kotlin SQL framework.

**What TourVerse uses it for:** Table objects describe the database schema in
Kotlin, while repository and service code uses typed query, insert, update, and
delete operations. Exposed does not replace Flyway; Flyway owns schema history,
while Exposed is used by application code to access the schema.

### Flyway

**What it is:** Flyway is a database migration tool.

**What TourVerse uses it for:** Versioned SQL files evolve the schema in a
repeatable order. On backend startup, Flyway validates previously applied
migrations and applies any pending migrations.

An applied migration is treated as immutable. A later change is introduced as
a new migration instead of rewriting production history.

### Migration history

The migration sequence records the main growth of the data model:

| Migration | Main purpose |
|---|---|
| V1 | Initial schema foundation |
| V2 | Destination storage |
| V3 | Users and refresh tokens |
| V4 | Expanded user profiles |
| V5 | Categories |
| V6 | Reviews |
| V7 | Favourites |
| V8 | Trips and trip destinations |
| V9 | Tourism services |
| V10 | Bookings |
| V11 | Notifications |
| V12 | Administration-oriented indexes |
| V13 | Global destination catalogue and import workflow |
| V14 | Cache state, verification, source metadata, and field provenance |

This is why migrations are more than setup scripts: collectively, they are a
technical history of the database.

---

## 5. Building authentication and user management

TourVerse then gained account registration, login, refresh sessions, logout,
profiles, roles, account deletion, and password changing.

### Password hashing and PBKDF2

**What password hashing is:** A password hash is a one-way derived value. The
original password is not stored and cannot be retrieved from the database.

**What PBKDF2 is:** Password-Based Key Derivation Function 2 repeatedly applies
a cryptographic function to a password and random salt. Repetition makes
large-scale guessing more expensive.

**How TourVerse uses it:** Passwords are hashed with PBKDF2-HMAC-SHA256, a
random salt, and a stored iteration count. Login hashes the supplied password
using the stored parameters and compares the result in constant-time style.

Password validation requires 8 to 128 characters with uppercase, lowercase,
and numeric characters. A private PowerShell helper allows a user to change a
password without putting it into command history. Password changes verify the
current password and revoke refresh sessions.

### JWT access tokens

**What they are:** A JSON Web Token is a signed compact token containing
claims, such as a user ID, role, issue time, and expiry.

**What TourVerse uses them for:** Short-lived access tokens authenticate
protected API requests. The signature prevents a client from changing its user
ID or role without invalidating the token.

JWTs are not encryption. Production therefore requires HTTPS and a strong,
private signing secret.

### Refresh tokens

**What they are:** Refresh tokens are longer-lived credentials used to obtain a
new access token without entering the password again.

**What TourVerse uses them for:** TourVerse stores only a hash of each refresh
token. Refresh rotates the token, logout revokes one session, and logout-all or
password changing revokes all refresh sessions for the user.

### Authentication and authorization

**Authentication** answers: “Who is making this request?”

**Authorization** answers: “Is that user allowed to perform this operation?”

TourVerse uses roles including `USER`, `ADMIN`, `TOUR_GUIDE`, and
`BUSINESS_OWNER`. Role checks protect administration, catalogue review, category
management, and booking-status operations. Ownership checks prevent one user
from modifying another user's private trip, booking, review, or managed
service.

---

## 6. Implementing the core REST API

### REST

**What it is:** Representational State Transfer is an architectural style for
resource-oriented HTTP APIs.

**What TourVerse uses it for:** Resources have predictable URLs and HTTP
methods:

- `GET` retrieves data.
- `POST` creates resources or starts operations.
- `PUT` updates or performs idempotent state changes.
- `DELETE` removes or deactivates resources.

The API uses HTTP status codes and a standard `ApiMessage` error shape so both
clients can respond consistently.

### UUID identifiers

**What they are:** Universally unique identifiers are 128-bit identifiers that
can be generated without a central numeric sequence.

**What TourVerse uses them for:** Important backend resources use UUIDs.
Android and web represent those UUIDs as strings at the JSON boundary.

### Validation and centralized error handling

Requests are validated before persistence. Examples include email format,
password strength, coordinate ranges, URL schemes, prices, currencies, rating
ranges, pagination bounds, and allowed roles or service types.

Ktor `StatusPages` converts expected exceptions into stable JSON errors and
handles unexpected failures without exposing stack traces to clients.

### Destinations

Destination functionality grew from ordinary CRUD into a searchable,
paginated catalogue. The current contract supports:

- UUID IDs.
- Name, country, country code, nullable city, description, and category.
- Nullable latitude and longitude.
- Nullable cover image URL.
- Backend timestamps.
- Pagination metadata.
- Search, country, city, category, sorting, and page-size parameters.
- Origin, verification, attribution, and map-availability metadata.

Administrative writes remain protected while public reads use approved
database records.

### Categories

Categories provide managed classifications for destinations. Administrators
can create, read, update, and delete category records. Non-administrator
mutation attempts are rejected.

### Reviews

Authenticated users can create, update, and delete reviews. Ratings contribute
to destination rating summaries and review counts. Ownership rules ensure
users manage their own reviews.

### Favourites

Favourites associate a user with destinations they want to keep. Users can add,
list, and remove their own favourites.

### Trips

Trips let users group destinations into personal itineraries. A user can
create, update, retrieve, and delete trips, then add or remove destinations.
Trip ownership is private to the authenticated account.

### Tourism services

Tourism services represent offerings connected to destinations, such as
accommodation, activities, or guide-related services. Public users can browse
active services. Authorized owners can manage their services, while
administrators can inspect broader platform data.

### Bookings

Users can book tourism services, view their bookings, and cancel eligible
bookings. Pricing is calculated by backend rules instead of trusting a client
supplied total. Administrators can inspect bookings and update booking status.

### Notifications

Notifications communicate relevant booking and platform events. Users can list
notifications, mark one as read, and mark all as read.

### Administration

Administrator endpoints provide statistics, user lists, role changes, service
inspection, booking inspection and status changes, categories, and destination
catalogue review. Authorization tests verify that non-administrators cannot
reach these operations.

---

## 7. Documenting and manually validating the API

### OpenAPI

**What it is:** OpenAPI is a machine-readable description format for HTTP APIs.

**What TourVerse uses it for:** The specification documents routes, parameters,
authentication, requests, responses, and schemas. The backend exposes the
specification and an API documentation page under `/api/openapi.yaml` and
`/api/docs`.

### Postman

**What it is:** Postman is an API development and manual-testing tool.

**What TourVerse uses it for:** The Postman collection provides reusable
requests for exercising the REST API. Tokens are represented with collection
variables rather than committed credentials.

Manual validation covered the principal resource workflows, persistence,
role boundaries, and rejected mutations. This produced strong confidence, but
the project deliberately avoids calling manual testing “literal 100% coverage”
unless every validation branch, ownership boundary, malformed request, token
case, and operational control appears in a traceable test matrix.

---

## 8. Creating the Android application

The Android client was created as a native Kotlin application. It communicates
with the backend and presents mobile versions of authentication, destination,
profile, favourite, review, trip, map, and related flows.

### Android

**What it is:** Android is Google's mobile operating system and application
platform.

**What TourVerse uses it for:** It provides the mobile runtime, application
manifest, resource system, lifecycle, navigation, networking permissions, and
application packaging.

### Jetpack Compose

**What it is:** Jetpack Compose is Android's declarative UI toolkit.

**What TourVerse uses it for:** Screens are functions that describe UI from
state. Compose renders forms, lists, loading states, errors, destination cards,
details, navigation, and map content without XML layout files.

### Material 3

**What it is:** Material 3 is Google's design system and Compose component
library.

**What TourVerse uses it for:** It supplies accessible interface components,
typography, buttons, fields, navigation elements, cards, and theme primitives.

### ViewModel and lifecycle components

**What they are:** Android ViewModels retain presentation state across
configuration changes. Lifecycle components coordinate work with the visible
screen lifecycle.

**What TourVerse uses them for:** ViewModels call repositories, track loading,
success, empty, and error states, and expose state to Compose screens without
placing networking logic directly inside UI functions.

### Repository pattern

**What it is:** A repository is an abstraction between application logic and
data sources.

**What TourVerse uses it for:** Android repositories call typed API interfaces
and present application-friendly results to ViewModels. This makes screens less
dependent on networking details and easier to test.

### Ktor Client and OkHttp

**What they are:** Ktor Client is a Kotlin HTTP client framework. OkHttp is the
Android-compatible HTTP engine used underneath it.

**What TourVerse uses them for:** They send JSON requests, attach
authentication, parse responses, and connect Android screens to the Ktor
backend.

### Kotlin coroutines

**What they are:** Coroutines are Kotlin's structured concurrency mechanism.

**What TourVerse uses them for:** Network and repository work runs
asynchronously without blocking the main UI thread.

### Navigation Compose

**What it is:** Navigation Compose manages screen destinations and back-stack
navigation in a Compose application.

**What TourVerse uses it for:** It connects authentication, home, destination,
community, profile, and detail screens while preserving navigation state.

### Coil

**What it is:** Coil is an image-loading library designed for Kotlin and
Compose.

**What TourVerse uses it for:** It loads destination and profile images from
URLs and integrates with Compose presentation.

### Android product flavours

**What they are:** Product flavours create variants of one Android application
with different build-time configuration.

**What TourVerse uses them for:**

- `development` connects through local development settings.
- `emulator` uses the emulator's host-machine address.
- `physical` accepts a LAN-accessible backend URL.
- `production` requires an HTTPS deployed URL.

Local variants explicitly permit HTTP for development. Production uses a
network-security configuration that rejects cleartext traffic.

### Android secure session storage

The Android client centralizes session handling and uses encrypted local token
storage. This is safer than scattering tokens across screens or storing them
as ordinary plain text preferences.

### Google Maps Compose

**What it is:** Maps Compose is the Compose integration for the Google Maps SDK
for Android.

**What TourVerse uses it for:** Destination details show a marker only when the
backend contains valid coordinates and the build has a restricted Android Maps
key. Missing configuration or coordinates produces a usable fallback and an
encoded external Maps link.

### Android signing

**What it is:** Android requires application packages to be cryptographically
signed. The signature establishes the publisher identity used for future
updates.

**What TourVerse uses it for:** Production release builds require an external
keystore path, alias, and passwords. These values are never stored in Git.
Release builds also enable code shrinking and resource shrinking.

---

## 9. Creating the web application

The browser client was created as a React single-page application that consumes
the same backend contract as Android.

### HTML and CSS

**What they are:** HTML describes browser content structure. CSS controls
layout, responsiveness, colour, spacing, and visual presentation.

**What TourVerse uses them for:** The web shell, forms, cards, destination
layouts, status messages, navigation, responsive behavior, and map fallbacks
are rendered and styled for browser users.

### JavaScript

**What it is:** JavaScript is the programming language executed by web
browsers.

**What TourVerse uses it for:** The compiled web application handles user
interaction, browser navigation, API requests, session state, and dynamic UI.

### TypeScript

**What it is:** TypeScript extends JavaScript with static types.

**What TourVerse uses it for:** API models, query parameters, component props,
session state, and response handling are checked during the build. This helped
identify and replace the old destination contract.

### React

**What it is:** React is a component-based library for building browser user
interfaces.

**What TourVerse uses it for:** Pages are composed from reusable components.
State updates cause React to render loading, success, empty, or error views.

### React Router

**What it is:** React Router maps browser URLs to React pages.

**What TourVerse uses it for:** It provides home, authentication, destination
details, favourites, trips, profiles, administration, and protected routes
without reloading the entire website.

### Vite

**What it is:** Vite is a web development server and production bundler.

**What TourVerse uses it for:** Vite provides fast local development, injects
approved build-time environment variables, and produces optimized static files
for production.

### Vitest

**What it is:** Vitest is a Vite-compatible JavaScript and TypeScript testing
framework.

**What TourVerse uses it for:** It tests API query construction, response/error
behavior, session refresh coordination, and destination map utilities.

### Web session management

The web client centralizes access and refresh-token behavior. It refreshes
access when possible and clears invalid sessions consistently. Refresh tokens
are currently JavaScript-readable because the backend returns tokens in JSON;
an HttpOnly secure-cookie design remains a possible future hardening step.

### Google Maps JavaScript API

**What it is:** Google's browser API for rendering interactive maps.

**What TourVerse uses it for:** Destination details can render a marker from
stored backend coordinates. A shared script loader prevents duplicate loading.
Missing keys, coordinates, invalid coordinates, or provider failure preserve
the rest of the detail page.

---

## 10. Aligning the frontend and backend contract

One significant development stage was the destination contract migration. The
backend had evolved beyond the older Android and web models. Both clients were
updated together rather than redesigning the backend.

The migration included:

- UUID destination identifiers represented as strings in clients.
- `PagedDestinationResponse` instead of a plain list.
- Country and nullable city.
- Nullable latitude and longitude.
- Nullable cover image URL.
- Backend `createdAt` and `updatedAt` timestamps.
- Page, page size, search, country, city, category, and sorting parameters.
- Standard backend error-response parsing.

Repositories, ViewModels, hooks, pages, screens, and tests were updated to use
the same contract. This stage demonstrated why an API contract matters: a
working backend and a working UI can still fail together when their models no
longer agree.

---

## 11. Growing the global destination catalogue

The initial database contained a limited development catalogue. TourVerse then
added a controlled process for discovering global destinations without making
the public application dependent on external providers.

### Database-first architecture

**What it means:** Public Android and web requests read TourVerse's database.
They do not wait for Wikidata, OpenTripMap, Google Places, or an AI model.

**Why it matters:** Public performance and availability remain under
TourVerse's control. Provider outages, quotas, changing schemas, and latency do
not directly break ordinary catalogue browsing.

### Provider abstraction

**What it is:** An interface defines the behavior expected from a destination
data provider without tying services to one company.

**What TourVerse uses it for:** Wikidata and OpenTripMap-related code can expose
normalized candidates through a common contract. Additional providers can be
introduced later without rewriting the public destination API.

### Wikidata

**What it is:** Wikidata is a collaboratively maintained structured knowledge
base operated by the Wikimedia Foundation.

**What TourVerse uses it for:** Bounded administrative discovery searches find
potential destination records and source references. Results become review
candidates rather than public destinations automatically.

### SPARQL

**What it is:** SPARQL is a query language for RDF graph data.

**What TourVerse uses it for:** The Wikidata integration uses bounded SPARQL
queries to discover entities with tourism-relevant structured data.

### OpenTripMap

**What it is:** OpenTripMap is a tourism and points-of-interest data service.

**What TourVerse uses it for:** TourVerse contains a provider boundary and
fixture parser for OpenTripMap-style data. Live use remains disabled unless an
appropriate API key and policy decision are supplied.

### Google Places

**What it is:** Google Places provides place identification, addresses,
coordinates, and related place metadata.

**What TourVerse uses it for:** Administrators can perform a bounded transient
search and explicitly link a chosen Place ID. The backend requests and stores
only minimal permitted information—place identification, address/location,
Maps URI, attribution, and source timestamps. Raw provider responses are not
persisted.

### Candidate review workflow

External results enter import batches and candidate records. Administrators can
inspect source data, duplicates, categories, and image/licensing concerns,
then approve, reject, edit, retry, or link candidates.

No provider result is automatically trusted merely because it was returned by
an API.

---

## 12. Adding cache, provenance, and verification

Global data needs to explain where it came from, how current it is, and whether
TourVerse has reviewed it.

### Cache

**What it is:** A cache stores previously acquired data for faster and more
reliable reuse.

**What TourVerse uses it for:** Destination records include cache state,
verification time, and expiry metadata. Refresh decisions happen in
administrative workflows rather than public read requests.

### Freshness policy

**What it is:** A freshness policy defines how long data is considered current
before review or refresh is needed.

**What TourVerse uses it for:** Core destination content and coordinates can
use different refresh periods. Stale records can be listed for controlled
administrative processing.

### Provenance

**What it is:** Provenance records the origin and history of data.

**What TourVerse uses it for:** Destination-level origin, provider references,
attribution, provider update times, hashes, active status, and field-level
source records explain why a value exists.

### Verification

**What it is:** Verification state records the level of confidence or editorial
review applied to a record.

**What TourVerse uses it for:** Records can be verified, partially verified,
awaiting review, or rejected. Confidence and editorial locks help prevent
external refreshes from overwriting deliberate TourVerse curation.

### Content hashing

**What it is:** A cryptographic content hash is a deterministic fingerprint of
normalized data.

**What TourVerse uses it for:** SHA-256 hashes help detect meaningful content
changes without storing or repeatedly comparing raw provider payloads.

### Merge precedence

Curated TourVerse values take priority. External content can fill acceptable
gaps or become a review candidate, but provider refreshes do not silently
replace editorially locked information.

---

## 13. Testing and quality assurance

TourVerse combines automated tests, compilation, manual API validation, and
live integration checks.

### Unit testing

**What it is:** A unit test verifies a small piece of behavior in isolation.

**TourVerse examples:** Password hashing, validation, query construction,
country-code handling, duplicate detection, content hashing, freshness, merge
rules, map utilities, and session behavior.

### Route testing

**What it is:** A route test sends simulated HTTP requests through a test
application.

**TourVerse examples:** Health and documentation endpoints, anonymous versus
authenticated access, administrator-only destination mutations, catalogue
imports, and rejected authorization without repository mutation.

### Integration testing

**What it is:** Integration testing verifies that multiple real components work
together.

**TourVerse examples:** Live PostgreSQL migrations, API workflows, persistence,
role boundaries, password changing, frontend builds against the backend
contract, and signed Android release validation.

### Test doubles and fixtures

**What they are:** Fakes and fixtures provide controlled data or implementations
for tests.

**What TourVerse uses them for:** Provider parsers and authorization paths can
be tested without relying on paid APIs, unpredictable networks, or real
external mutations.

### Current verified build baseline

At the time this journal was created:

- Backend: 53 automated tests passing.
- Web: 13 automated tests passing.
- Android: 88 automated test executions passing across variants.
- Web production build: passing.
- Android development, emulator, and physical debug APKs: building.
- Android production App Bundle path: verified with a disposable signing key.

These results are a strong baseline, not a promise that future changes cannot
introduce regressions. Continuous integration exists to repeat these checks.

---

## 14. Security hardening

### CORS

**What it is:** Cross-Origin Resource Sharing controls which browser origins
may call an API.

**What TourVerse uses it for:** Development can be permissive locally.
Production requires explicit HTTPS origins and rejects wildcard configuration.

### Rate limiting

**What it is:** Rate limiting restricts how many requests a client can make in
a period.

**What TourVerse uses it for:** The backend applies a configurable per-minute
limit to reduce accidental overload and basic abuse. A distributed production
deployment may later move this control to a shared gateway or data store.

### HTTPS and TLS

**What they are:** HTTPS is HTTP protected by Transport Layer Security. It
encrypts traffic and authenticates the server certificate.

**What TourVerse uses them for:** Production credentials, tokens, profiles, and
bookings must travel over HTTPS. Caddy provides the public TLS boundary, the
backend emits HSTS in production, and Android production rejects cleartext
HTTP.

### Security headers

The backend and web server add headers such as:

- `X-Content-Type-Options` to reduce content-type interpretation attacks.
- `X-Frame-Options` to prevent framing.
- `Referrer-Policy` to limit referrer leakage.
- `Permissions-Policy` to disable unused browser capabilities.
- `Strict-Transport-Security` in backend production responses.

### Environment variables and secret management

**What they are:** Environment variables provide runtime configuration outside
source code.

**What TourVerse uses them for:** Database credentials, JWT secrets, allowed
origins, provider keys, Maps keys, API URLs, and Android signing values are
provided externally.

Example files contain placeholders only. Real values must be entered directly
into a private local environment or hosting provider's secret manager.

### Structured boundaries

TourVerse keeps:

- Provider keys on the backend.
- Raw passwords out of the database.
- Passwords out of shell history through secure prompts.
- Refresh tokens hashed in PostgreSQL.
- Android keystores and signing passwords outside Git.
- Development data disabled in production.
- External candidates behind administrator review.

---

## 15. Preparing deployment

### Docker

**What it is:** Docker packages an application and its runtime dependencies
into a container image.

**What TourVerse uses it for:** The backend image is built with Gradle and runs
on a smaller Java runtime as a non-root user. The web image is built with Node
and served by Nginx.

### Multi-stage Docker build

**What it is:** A multi-stage build uses one image to compile software and a
different smaller image to run the result.

**What TourVerse uses it for:** Gradle and Node development dependencies do not
need to remain in the final backend or web runtime images.

### Docker Compose

**What it is:** Docker Compose declares and runs multiple related containers.

**What TourVerse uses it for:** The deployment stack coordinates PostgreSQL,
the Ktor API, Nginx web server, Caddy proxy, volumes, environment variables,
health checks, dependencies, and restart policies.

### Nginx

**What it is:** Nginx is a web server and reverse proxy.

**What TourVerse uses it for:** It serves the compiled React files, provides
single-page application route fallback, caches versioned assets, and applies
browser security headers.

### Caddy

**What it is:** Caddy is a web server and reverse proxy with automatic HTTPS.

**What TourVerse uses it for:** It accepts public traffic, obtains and renews
TLS certificates when DNS is correct, sends `/api/*` traffic to Ktor, and sends
other paths to the web application.

### Health checks

**What they are:** Health checks repeatedly determine whether a service is
ready and responding.

**What TourVerse uses them for:** PostgreSQL uses `pg_isready`; the API calls
`/api/health`; the web container checks its root page. Compose uses these
results when ordering startup and reporting container health.

### Persistent volumes

**What they are:** Docker volumes retain data independently of a container's
temporary filesystem.

**What TourVerse uses them for:** PostgreSQL data and Caddy certificate state
survive container replacement.

Volumes are not backups. Production still requires scheduled encrypted
off-server backups and tested restoration.

---

## 16. Adding continuous integration

### Continuous integration

**What it is:** CI automatically builds and tests changes in a clean
environment.

**What TourVerse uses it for:** GitHub Actions runs independent jobs for:

- Backend clean build and tests on Java 17.
- Web dependency installation, tests, and production build on Node 22.
- Android tests and non-release debug variants on Java 21.

### GitHub Actions

**What it is:** GitHub Actions is GitHub's workflow automation service.

**What TourVerse uses it for:** `.github/workflows/ci.yml` runs on changes to
the main branch, pull requests, or manual dispatch. Dependency caching reduces
repeated download time. Workflow permissions are read-only unless a later
deployment workflow explicitly needs more.

CI does not replace staging. It proves that source builds and automated tests
pass; it cannot confirm real DNS, TLS, production data, provider quotas, device
behavior, or cloud resource configuration.

---

## 17. Logging and operational behavior

### Logback

**What it is:** Logback is a JVM logging implementation.

**What TourVerse uses it for:** Ktor and backend services record startup,
migration, request, and error information using configurable log levels.

### Request logging

Request logging records method, path, status, and timing information useful for
diagnosis. Health calls are filtered to reduce noise. Passwords, authorization
headers, API keys, and token bodies must never be added to logs.

### Forwarded headers

**What they are:** Reverse proxies add headers describing the original client
and protocol.

**What TourVerse uses them for:** The backend understands traffic forwarded
through the public proxy. Production infrastructure must ensure clients cannot
bypass or spoof the trusted proxy boundary.

### Backups and rollback

The deployment runbook requires:

- A backup before releases.
- Scheduled off-server backups.
- Periodic restoration tests.
- Review of new Flyway migrations.
- Tagged application revisions.
- Additive, backward-compatible database changes where possible.

Application containers can be returned to an earlier revision. Database
rollback is more complex and must account for data created after a backup.

---

## 18. Problems encountered and lessons learned

### Port conflicts

The backend sometimes failed with `java.net.BindException: Address already in
use`. This meant another process already owned the configured port—not that
Flyway or PostgreSQL had failed.

The project standardized local backend use on port `8081` and aligned Android,
web, Docker, OpenAPI, and Postman configuration with that port. The lasting
lesson is to inspect the owning process before stopping it or changing ports.

### SQL entered into PowerShell

SQL such as `ALTER ROLE` cannot be executed directly as a PowerShell command.
It must run through PostgreSQL tooling such as `psql` or a database client.

### Password retrieval

An existing password could not be retrieved because the database contained
only its PBKDF2 hash. The correct operation was a controlled password reset or
authenticated password change—not attempting to reverse the hash.

### Frontend destination mismatch

Android and web originally used an obsolete destination model. The solution
was a coordinated frontend contract migration covering UUIDs, nullable fields,
timestamps, pagination, search, filters, sorting, and error responses.

### Searching returned too few results

The number of visible results was related to the records available in the
database and query behavior, not merely the search box. This led to the global
catalogue foundation and controlled provider-import workflow.

### Avoiding provider-dependent public pages

Calling Wikipedia, Wikidata, OpenTripMap, or Google Places directly from public
client screens would have introduced latency, quotas, inconsistent schemas,
key exposure, and availability risks. TourVerse instead imports into review,
stores approved data, and serves public reads from PostgreSQL.

### Avoiding premature AI complexity

The roadmap considered multiple LLM providers and embeddings, but the better
sequence was to finish the core application and deployment foundation first.
The intended future approach is one provider interface, one initial provider,
database-grounded recommendations, and later evaluation of embeddings or
provider fallback only when justified.

### Honest testing claims

Comprehensive manual testing was initially described too strongly. The
corrected assessment distinguishes strong workflow confidence from literal
100% branch and operational coverage.

### Protecting credentials

A concrete bearer token was discovered in a Postman definition during
production hardening. It was replaced with a collection variable and the
repository was scanned for credential patterns. This reinforced the need to
audit test tools as carefully as application source.

### Build environment issues

Windows file locks, Gradle daemon caches, restricted dependency downloads, and
SDK tool-version warnings occasionally affected verification. These were
separated from real source failures by stopping only relevant daemons, using
clean builds, checking exit codes, and rerunning in controlled environments.

---

## 19. Current TourVerse state

### Implemented backend areas

- Health and API documentation.
- Registration, login, token refresh, logout, logout-all, password changing,
  profile management, image updates, and account deletion.
- Destination CRUD, details, pagination, search, filtering, sorting, countries,
  cache state, verification, and attribution.
- Category management.
- Reviews and rating summaries.
- Favourites.
- Private trips and trip destinations.
- Tourism services.
- Bookings and booking-status workflows.
- Notifications.
- Administration statistics, users, roles, services, bookings, and categories.
- Destination discovery, candidates, approval/rejection, duplicates, source
  references, refresh jobs, stale records, and Google Place linking.

### Implemented client areas

Both clients support the current destination contract and important
authentication, profile, destination, favourite, review, and trip-related
flows. The web client additionally exposes destination catalogue
administration. Some backend platform modules still need complete user-facing
Android and web portals.

### Implemented production foundations

- Backend and web container images.
- PostgreSQL/API/web/Caddy Compose topology.
- HTTPS routing and production configuration validation.
- Android HTTPS policy and private signing gate.
- CI for all three applications.
- Backup, update, rollback, and staging checklist documentation.

### Known remaining work

- Select a hosting provider or Linux server.
- Select and configure a domain.
- Decide between managed PostgreSQL and self-hosted PostgreSQL.
- Enter real secrets only in private secret-management interfaces.
- Curate and approve real production destination data.
- Deploy and test staging.
- Configure monitoring, alerts, log retention, and scheduled backups.
- Publish privacy, terms, support, and data-retention policies.
- Complete remaining Android and web screens for all backend modules.
- Perform browser, physical-device, accessibility, malformed-input, token,
  ownership-isolation, load, backup-restore, and production-startup testing.
- Create and safeguard the real Android upload keystore.
- Complete Google Play Console preparation if Play Store release is intended.

---

## 20. Future AI direction

AI is intentionally not part of the current production foundation.

The recommended first implementation is:

```text
AiProvider interface
        |
        v
One initial provider
        |
        v
Database-grounded destination guide
```

### Large language model

**What it is:** An LLM is a machine-learning model trained to understand and
generate language.

**Potential TourVerse use:** It could explain or recommend destinations from
approved TourVerse data. It should not invent bookable services, prices, or
availability.

### Retrieval-augmented generation

**What it is:** RAG retrieves trusted source material and supplies it to a
language model before generating an answer.

**Potential TourVerse use:** PostgreSQL search can initially retrieve
destinations by country, category, or interests. The model can then answer
using those records.

### Embeddings and pgvector

**What they are:** Embeddings are numeric representations of semantic meaning.
pgvector is a PostgreSQL extension for storing and searching vectors.

**Potential TourVerse use:** They may later support semantic destination search
when the catalogue is large enough. They are deliberately postponed until
ordinary database retrieval is insufficient.

### Multi-provider routing

Multiple LLM providers are also postponed. TourVerse should first prove one
useful AI feature, evaluation method, safety policy, quota, and cost model.
The provider interface can preserve future flexibility without requiring
premature routing complexity.

---

## 21. How the complete system works today

```text
Android app                         Web browser
     |                                  |
     | HTTPS JSON                       | HTTPS JSON
     +----------------+-----------------+
                      |
                      v
                 Caddy proxy
                      |
          +-----------+-----------+
          |                       |
          v                       v
      Ktor API                Nginx + React
          |
          v
      HikariCP
          |
          v
      PostgreSQL

Administrative background/import flow:

Wikidata / permitted providers
          |
          v
Normalized candidate
          |
          v
Duplicate and validation checks
          |
          v
Administrator review
          |
          v
Approved TourVerse destination
          |
          v
Public database-first API
```

The essential architectural decision is that TourVerse owns the public
experience. External systems assist controlled data acquisition, but approved
PostgreSQL records, backend rules, and documented contracts determine what
users see.

---

## 22. Technology reference summary

| Technology or concept | What it provides in TourVerse |
|---|---|
| Kotlin | Backend and Android implementation language |
| JVM/JDK | Backend runtime, compiler, cryptography, and build tools |
| Ktor Server | REST routing, plugins, JSON, errors, and HTTP serving |
| Netty | Asynchronous backend network engine |
| Ktor Client | Android and backend provider HTTP requests |
| Kotlin serialization | Typed JSON encoding and decoding |
| Coroutines | Non-blocking asynchronous Kotlin work |
| Gradle | Backend and Android builds, dependencies, tests, packaging |
| PostgreSQL | Persistent relational system of record |
| JDBC | JVM-to-PostgreSQL communication standard |
| HikariCP | Reusable database connection pool |
| Exposed | Typed Kotlin database access |
| Flyway | Versioned database schema migration |
| PBKDF2-HMAC-SHA256 | Salted one-way password hashing |
| JWT | Signed short-lived access credentials |
| Refresh tokens | Renewable authenticated sessions |
| REST/JSON | Shared backend-client communication contract |
| UUID | Distributed-safe resource identifiers |
| OpenAPI | Machine-readable API documentation |
| Postman | Manual API requests and workflow validation |
| Android | Native mobile application platform |
| Jetpack Compose | Declarative Android interface |
| Material 3 | Android components and design primitives |
| ViewModel | Android presentation state holder |
| Navigation Compose | Android screen navigation |
| OkHttp | Android HTTP engine |
| Coil | Android image loading |
| Product flavours | Environment-specific Android variants |
| Maps Compose | Native Android destination maps |
| HTML/CSS | Web structure and presentation |
| TypeScript | Type-safe browser application code |
| React | Component-based web interface |
| React Router | Browser navigation and protected routes |
| Vite | Web development server and production bundler |
| Vitest | Web unit testing |
| Google Maps JavaScript API | Browser destination maps |
| Wikidata | Structured external discovery source |
| SPARQL | Wikidata graph query language |
| OpenTripMap | Optional tourism-data provider boundary |
| Google Places | Minimal administrator-approved place linking |
| Cache metadata | Freshness and refresh decisions |
| Provenance | Source and field-origin history |
| SHA-256 | Deterministic content fingerprints |
| Docker | Reproducible application containers |
| Docker Compose | Multi-container orchestration |
| Nginx | Production static web serving |
| Caddy | Public reverse proxy and automatic HTTPS |
| GitHub Actions | Automated full-stack CI |
| Logback | Backend logging |
| CORS | Browser-origin access control |
| Rate limiting | Basic request-volume protection |
| TLS/HTTPS | Encrypted and authenticated network transport |
| Android signing | Publisher identity and trusted application updates |

---

## 23. Phase-by-phase file creation and modification journal

This section connects the development story to the repository files. It uses
the available Git history wherever possible:

- **Added** means the file first appeared in that recorded phase.
- **Modified** means an existing file was changed to support the phase.
- **Deleted/replaced** means the earlier implementation was intentionally
  removed after another implementation took responsibility.
- A grouped directory row is used where a tool generated a large family of
  closely related files, such as the Postman request tree.

The commit labels `Phase 1` through `Phase 5` were kept as they appear in Git.
The explanations below give those otherwise short commit messages their
technical meaning.

The original commits stored the clients at root-level paths named
`androidApp/` and `webApp/`. The file tables below use their current locations,
`frontend/androidApp/` and `frontend/webApp/`, after the later frontend-folder
consolidation. Git history still records the original paths accurately.

### Recorded phase 0 — Initial TourVerse project structure

**Git evidence:** `b04fd97`, “Initial TourVerse project structure”.

This phase established all three applications, the shared documentation
folder, and a simple destination flow before PostgreSQL persistence and
authentication were introduced.

#### Repository and documentation files

| File | Change | What it held at this stage |
|---|---|---|
| `.gitignore` | Added | Initial exclusions for IDE state, builds, dependencies, environment files, and local configuration. |
| `README.md` | Added | Root introduction, project layout, and basic commands for the three applications. |
| `docs/ARCHITECTURE.md` | Added | First cross-application architecture description. |
| `docs/API_CONTRACT.md` | Added | First written agreement between clients and backend. |
| `docs/PROJECT_TREE.txt` | Added | Human-readable inventory of source files. |
| `backend/README.md` | Added | Backend-specific structure and startup notes. |
| `frontend/androidApp/README.md` | Added | Android setup and implementation notes. |
| `frontend/webApp/README.md` | Added | Web setup and implementation notes. |

#### Initial backend files

| File | Change | What it held at this stage |
|---|---|---|
| `backend/build.gradle.kts` | Added | Kotlin, Ktor, serialization, logging, application, and test build configuration. |
| `backend/settings.gradle.kts` | Added | Backend Gradle project name and plugin repositories. |
| `backend/src/main/kotlin/com/tourverse/Application.kt` | Added | Ktor application entry point and plugin installation order. |
| `backend/src/main/resources/application.conf` | Added | Netty host, port, and application module configuration. |
| `backend/src/main/resources/logback.xml` | Added | Backend logging format and levels. |
| `backend/src/main/kotlin/com/tourverse/dto/ApiMessage.kt` | Added | Standard status/message response used by success and error paths. |
| `backend/src/main/kotlin/com/tourverse/models/Destination.kt` | Added | Initial destination response model. |
| `backend/src/main/kotlin/com/tourverse/repositories/DestinationRepository.kt` | Added | Contract for destination persistence operations. |
| `backend/src/main/kotlin/com/tourverse/repositories/InMemoryDestinationRepository.kt` | Added | Temporary destination storage used before PostgreSQL became authoritative. |
| `backend/src/main/kotlin/com/tourverse/services/DestinationService.kt` | Added | Initial destination business operations between routes and repository. |
| `backend/src/main/kotlin/com/tourverse/routes/DestinationRoutes.kt` | Added | Initial destination HTTP endpoints. |
| `backend/src/main/kotlin/com/tourverse/routes/HealthRoutes.kt` | Added | `/api/health` readiness endpoint. |
| `backend/src/main/kotlin/com/tourverse/plugins/Routing.kt` | Added | Central route registration. |
| `backend/src/main/kotlin/com/tourverse/plugins/HTTP.kt` | Added | Initial browser HTTP/CORS behavior. |
| `backend/src/main/kotlin/com/tourverse/plugins/Serialization.kt` | Added | JSON serialization configuration. |
| `backend/src/main/kotlin/com/tourverse/plugins/StatusPages.kt` | Added | Initial centralized error-response handling. |
| `backend/src/test/kotlin/com/tourverse/ApplicationTest.kt` | Added | First Ktor health-route test. |

The initial `.gitkeep` files under empty backend packages reserved intended
locations for controllers, database tables, security, and utilities. They were
later replaced by real source files.

#### Initial Android files

| File | Change | What it held at this stage |
|---|---|---|
| `frontend/androidApp/build.gradle.kts` | Added | Root Android plugin configuration. |
| `frontend/androidApp/settings.gradle.kts` | Added | Android modules and dependency repositories. |
| `frontend/androidApp/gradle.properties` | Added | Gradle and Android build properties. |
| `frontend/androidApp/app/build.gradle.kts` | Added | Application ID, SDK values, Compose configuration, and dependencies. |
| `frontend/androidApp/app/src/main/AndroidManifest.xml` | Added | Internet permission, application metadata, and launcher activity. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/MainActivity.kt` | Added | Android launcher activity and Compose content root. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/data/model/Destination.kt` | Added | Initial Android destination DTO. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/data/remote/ApiClient.kt` | Added | Android Ktor Client construction. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/data/remote/TourismApi.kt` | Added | Initial typed destination API calls. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/data/repository/DestinationRepository.kt` | Added | Android data layer between API and ViewModel. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/ui/screens/HomeViewModel.kt` | Added | Destination loading and home-screen state. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/ui/screens/HomeScreen.kt` | Added | First mobile catalogue presentation. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/ui/theme/Theme.kt` | Added | Compose application theme. |
| `frontend/androidApp/app/src/main/res/values/strings.xml` | Added | Android string resources. |
| `frontend/androidApp/app/src/main/res/values/styles.xml` | Added | Android application theme resources. |

Empty `.gitkeep` files also reserved Android component, navigation, hook, page,
utility, and platform directories before those layers were implemented.

#### Initial web files

| File | Change | What it held at this stage |
|---|---|---|
| `frontend/webApp/package.json` | Added | React, TypeScript, Vite, and project scripts. |
| `frontend/webApp/index.html` | Added | Browser document and React mount element. |
| `frontend/webApp/vite.config.ts` | Added | Vite and React build integration. |
| `frontend/webApp/tsconfig.json` | Added | Root TypeScript project references. |
| `frontend/webApp/tsconfig.app.json` | Added | Browser application compiler rules. |
| `frontend/webApp/tsconfig.node.json` | Added | Vite configuration compiler rules. |
| `frontend/webApp/.env.example` | Added | Example browser API base URL. |
| `frontend/webApp/src/main.tsx` | Added | React application entry point. |
| `frontend/webApp/src/App.tsx` | Added | Initial application component and destination loading. |
| `frontend/webApp/src/models/Destination.ts` | Added | Initial TypeScript destination interface. |
| `frontend/webApp/src/services/api.ts` | Added | Initial destination fetch function. |
| `frontend/webApp/src/components/DestinationCard.tsx` | Added | Reusable destination summary card. |
| `frontend/webApp/src/styles/index.css` | Added | Initial global and destination-card styling. |

### Recorded phase 1 — Planning artifacts

**Git evidence:** `fa3325e`, “Phase 1”.

This commit did not introduce production source behavior. It captured generated
Android planning and implementation-assistance material.

| File | Change | What it held at this stage |
|---|---|---|
| `frontend/androidApp/.artifacts/.../implementation_plan.artifact.md` | Added | Proposed Android implementation order. |
| `frontend/androidApp/.artifacts/.../task.artifact.md` | Added | The scoped Android task description. |
| `frontend/androidApp/.artifacts/.../walkthrough.artifact.md` | Added | A generated walkthrough of the intended work. |

These artifacts are historical development aids. Runtime behavior comes from
the application source, not from the artifact documents.

### Recorded phase 2 — Reproducible builds and database bootstrap

**Git evidence:** `5dffd1e`, “Phase 2”.

This phase replaced an implicit developer-machine setup with Gradle wrappers,
real database initialization, the first Flyway migration, web dependency
locking, and stronger project configuration.

| File or group | Change | What it held at this stage |
|---|---|---|
| `backend/gradlew`, `backend/gradlew.bat` | Added | Platform-specific backend Gradle wrapper launchers. |
| `backend/gradle/wrapper/gradle-wrapper.jar` | Added | Wrapper bootstrap executable. |
| `backend/gradle/wrapper/gradle-wrapper.properties` | Added | Pinned backend Gradle distribution. |
| `backend/.env.example` | Added | Safe names and placeholders for local backend configuration. |
| `backend/build.gradle.kts` | Modified | Added PostgreSQL, Flyway, HikariCP, Exposed, dotenv, and related dependencies. |
| `backend/src/main/kotlin/com/tourverse/database/DatabaseFactory.kt` | Added | Loads database configuration, creates the Hikari pool, runs Flyway, and connects Exposed. |
| `backend/src/main/resources/db/migration/V1__initial_schema.sql` | Added | Established the first Flyway-managed schema baseline. |
| `backend/src/main/kotlin/com/tourverse/Application.kt` | Modified | Initialized the database before configuring request handling. |
| `backend/src/main/kotlin/com/tourverse/plugins/HTTP.kt` | Modified | Refined HTTP behavior for the expanding application. |
| `backend/src/main/kotlin/com/tourverse/plugins/Serialization.kt` | Modified | Expanded JSON configuration for typed models. |
| `backend/src/main/kotlin/com/tourverse/plugins/StatusPages.kt` | Modified | Improved normalized failure responses. |
| `backend/src/test/kotlin/com/tourverse/ApplicationTest.kt` | Modified | Kept application testing aligned with startup changes. |
| `frontend/androidApp/gradlew`, `frontend/androidApp/gradlew.bat` | Added | Platform-specific Android Gradle wrapper launchers. |
| `frontend/androidApp/gradle/wrapper/*` | Added | Pinned Android Gradle distribution and wrapper bootstrap. |
| `frontend/androidApp/gradle/gradle-daemon-jvm.properties` | Added | Android Gradle daemon JVM selection. |
| `frontend/androidApp/app/build.gradle.kts` | Modified | Expanded Android dependencies and build behavior. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/data/remote/TourismApi.kt` | Modified | Kept Android networking aligned with the backend. |
| `frontend/webApp/package-lock.json` | Added | Exact npm dependency-resolution lock file. |
| `frontend/webApp/src/vite-env.d.ts` | Added | Type declarations for Vite environment variables. |
| `frontend/webApp/tsconfig.node.json` | Modified | Corrected TypeScript/Vite build configuration. |
| `.gitignore` and the three READMEs | Modified | Documented and protected the new build/database setup. |

### Recorded phase 3 — Full backend platform and destination-contract expansion

**Git evidence:** `d16043b`, “Phase 3”.

This was the largest early feature phase. It replaced temporary destination
storage with PostgreSQL repositories, introduced authentication and platform
resources, added migrations V2–V12, expanded the destination contract, created
OpenAPI/Postman material, and aligned the first Android/web destination flows.

#### Database schema files

| File | Change | What it introduced |
|---|---|---|
| `V2__create_destinations_table.sql` | Added | Persistent destination records. |
| `V3__create_users_and_refresh_tokens.sql` | Added | Accounts, roles, password hashes, and refresh sessions. |
| `V4__expand_user_profiles.sql` | Added | Biography, nationality, interests, visibility, image, and profile metadata. |
| `V5__create_categories.sql` | Added | Administrator-managed destination categories. |
| `V6__create_reviews.sql` | Added | User ratings and review comments. |
| `V7__create_favorites.sql` | Added | User-to-destination favourite relationships. |
| `V8__create_trips.sql` | Added | Trips and ordered trip-destination membership. |
| `V9__create_tourism_services.sql` | Added | Destination-linked tourism service records and ownership. |
| `V10__create_bookings.sql` | Added | Bookings, price totals, dates, and status. |
| `V11__create_notifications.sql` | Added | User notification records and read state. |
| `V12__administration_indexes.sql` | Added | Indexes supporting administrative lists and statistics. |

All paths above are under
`backend/src/main/resources/db/migration/`.

#### Backend persistence and table files

| File | Change | What it held |
|---|---|---|
| `database/tables/DestinationsTable.kt` | Added | Exposed mapping for destinations. |
| `database/tables/UsersTable.kt` | Added | Exposed mappings for users and refresh tokens. |
| `database/tables/CategoriesTable.kt` | Added | Exposed category schema mapping. |
| `database/tables/CommunityTables.kt` | Added | Reviews, favourites, trips, and trip-destination mappings. |
| `database/tables/PlatformTables.kt` | Added | Tourism services, bookings, and notifications. |
| `repositories/PostgresDestinationRepository.kt` | Added | SQL-backed destination CRUD, filtering, paging, and sorting. |
| `repositories/PostgresCategoryRepository.kt` | Added | SQL-backed category operations. |
| `repositories/CategoryRepository.kt` | Added | Category persistence contract. |
| `repositories/DestinationRepository.kt` | Modified | Expanded the destination persistence contract for PostgreSQL and paging. |
| `repositories/InMemoryDestinationRepository.kt` | Deleted/replaced | Removed temporary storage after PostgreSQL became the system of record. |

The paths in this table are under
`backend/src/main/kotlin/com/tourverse/`.

#### Backend models and shared response files

| File | Change | What it held |
|---|---|---|
| `models/AuthModels.kt` | Added | Registration, login, refresh, logout, profile, user, and auth response DTOs. |
| `models/ProfileModels.kt` | Added | Extended profile, image, and account-deletion requests/responses. |
| `models/CategoryModels.kt` | Added | Category create, update, and response DTOs. |
| `models/CommunityModels.kt` | Added | Review, favourite, trip, and trip-destination DTOs. |
| `models/PlatformModels.kt` | Added | Service, booking, notification, statistics, and admin DTOs. |
| `models/CreateDestinationRequest.kt` | Added | Destination creation contract. |
| `models/UpdateDestinationRequest.kt` | Added | Optional destination update contract. |
| `models/DestinationQuery.kt` | Added | Validated pagination, search, filter, and sort parameters. |
| `models/Destination.kt` | Modified | Expanded persisted destination response fields. |
| `dto/PagedDestinationResponse.kt` | Added | Items plus page, size, total-items, and total-pages metadata. |
| `exceptions/ApiExceptions.kt` | Added | Authentication, authorization, not-found, conflict, and API exception types. |
| `utils/ApiExceptions.kt` | Added | Validation exception used for client input errors. |
| `utils/AppEnvironment.kt` | Added | Typed environment and dotenv configuration access. |
| `utils/Serializers.kt` | Added | UUID, instant, and related JSON serializers. |

#### Backend authentication and platform services

| File | Change | What it held |
|---|---|---|
| `security/PasswordHasher.kt` | Added | PBKDF2 hashing and verification. |
| `security/TokenService.kt` | Added | Access-token creation/validation and refresh-token hashing. |
| `security/Auth.kt` | Added | Bearer extraction, authenticated-user context, and role helpers. |
| `services/AuthService.kt` | Added | Registration, login, refresh rotation, logout, and current-user logic. |
| `services/AuthValidator.kt` | Added | Name, email, password, and basic-profile validation. |
| `services/UserProfileService.kt` | Added | Extended profile loading, updates, image changes, and deletion. |
| `services/ProfileValidator.kt` | Added | Profile-field and URL validation. |
| `services/CategoryService.kt` | Added | Category business operations. |
| `services/CategoryValidator.kt` | Added | Category name, description, and image validation. |
| `services/CommunityService.kt` | Added | Reviews, rating summaries, favourites, trips, and ownership rules. |
| `services/PlatformService.kt` | Added | Tourism services, bookings, notifications, statistics, and admin operations. |
| `services/DestinationService.kt` | Modified | Added validated paging, filtering, CRUD, and role-aware behavior. |
| `services/DestinationValidator.kt` | Added | Destination names, descriptions, URLs, and coordinate validation. |

#### Backend route and plugin files

| File | Change | What it held |
|---|---|---|
| `routes/AuthRoutes.kt` | Added | Registration, login, refresh, logout, and current-user routes. |
| `routes/UserProfileRoutes.kt` | Added | Extended profile, image, and account-deletion routes. |
| `routes/CategoryRoutes.kt` | Added | Public category reads and administrator mutations. |
| `routes/CommunityRoutes.kt` | Added | Reviews, favourites, trips, and trip-destination endpoints. |
| `routes/PlatformRoutes.kt` | Added | Services, bookings, notifications, and administrator endpoints. |
| `routes/DocumentationRoutes.kt` | Added | OpenAPI file and browser documentation endpoints. |
| `routes/DestinationRoutes.kt` | Modified | Paginated reads, UUID parsing, and protected destination writes. |
| `plugins/Routing.kt` | Modified | Registered all new route modules and service dependencies. |
| `plugins/Security.kt` | Added | Production validation and request-rate limiting. |
| `plugins/Observability.kt` | Added | Forwarded headers, compression, default headers, and request logging. |
| `plugins/HTTP.kt` | Modified | Environment-aware CORS behavior. |
| `plugins/StatusPages.kt` | Modified | Mapped the larger exception set to `ApiMessage` responses. |
| `Application.kt` | Modified | Installed the expanded application configuration. |
| `application.conf` | Modified | Aligned Ktor deployment configuration. |

#### Backend tests introduced or expanded

| File | Change | What it verified |
|---|---|---|
| `AuthValidatorTest.kt` | Added | Registration and password rules. |
| `CategoryValidatorTest.kt` | Added | Category validation. |
| `DestinationQueryTest.kt` | Added | Pagination, filters, and sort validation. |
| `DestinationValidatorTest.kt` | Added | Destination field and coordinate rules. |
| `ProfileValidatorTest.kt` | Added | Profile-field validation. |
| `DocumentationRoutesTest.kt` | Added | OpenAPI and documentation availability. |
| `security/PasswordHasherTest.kt` | Added | Password hashing and verification behavior. |
| `security/TokenServiceTest.kt` | Added | Token creation, validation, and tamper handling. |
| `security/TokenExpiryTest.kt` | Added | Expired-token behavior. |
| `services/PlatformValidationTest.kt` | Added | Service and platform validation rules. |
| `ApplicationTest.kt` | Modified | Retained health coverage with the expanded configuration. |

All test paths above are under
`backend/src/test/kotlin/com/tourverse/`.

#### Android destination-contract files

| File | Change | What it held |
|---|---|---|
| `data/model/ApiModels.kt` | Added | Shared Android API messages and paging structures. |
| `data/model/DestinationQuery.kt` | Added | Typed Android destination query options. |
| `data/model/Destination.kt` | Modified | UUID-string, nullable-field, country, coordinate, and timestamp contract. |
| `data/remote/ApiClient.kt` | Modified | Environment-aware Ktor Client behavior. |
| `data/remote/TourismApi.kt` | Modified | Paginated endpoint and query-parameter handling. |
| `data/repository/DestinationRepository.kt` | Modified | Paged/filterable destination operations. |
| `ui/screens/HomeViewModel.kt` | Modified | Pagination, search, filtering, retry, and status state. |
| `ui/screens/HomeScreen.kt` | Modified | Search/filter/pagination UI and explicit status views. |
| `test/.../DestinationModelTest.kt` | Added | Destination model serialization expectations. |
| `test/.../TourismApiTest.kt` | Added | Query construction and response/error handling. |

Paths in this table begin at
`frontend/androidApp/app/src/main/kotlin/com/tourverse/` unless marked `test`.

#### Web contract and API tooling files

| File or group | Change | What it held |
|---|---|---|
| `frontend/webApp/src/models/Destination.ts` | Modified | Current destination, query, paging, country, and error types. |
| `frontend/webApp/src/services/api.ts` | Modified | Safe query construction and standardized error parsing. |
| `frontend/webApp/src/services/api.test.ts` | Added | Web destination query and error tests. |
| `frontend/webApp/src/App.tsx` | Modified | Paginated destination loading and state transitions. |
| `frontend/webApp/src/components/DestinationCard.tsx` | Modified | Current nullable-field destination presentation. |
| `frontend/webApp/src/styles/index.css` | Modified | Expanded catalogue and status styling. |
| `backend/src/main/resources/openapi/tourverse-openapi.yaml` | Added | Backend-served API specification. |
| `postman/specs/TourVerse API/openapi.yaml` | Added | Postman-side copy of the API specification. |
| `postman/collections/TourVerse API/**` | Added | Generated request definitions and examples for authentication, users, destinations, categories, community, services, bookings, notifications, and admin. |
| `.postman/resources.yaml`, `.postman/workflows.yaml` | Added | Postman workspace/resource metadata. |
| `postman/globals/workspace.globals.yaml` | Added | Shared Postman workspace variables. |
| `backend/Dockerfile`, `backend/docker-compose.yml` | Added | Initial container packaging and local PostgreSQL/API orchestration. |
| `backend/.dockerignore` | Added | Removed builds, secrets, and unnecessary files from Docker build context. |
| `backend/.github/workflows/backend-ci.yml` | Added | Early backend-only CI workflow, later replaced by root full-stack CI. |

### Recorded maintenance phase — Android Studio build alignment

**Git evidence:** `6e15787`, “Android Studio edits”.

| File | Change | What it held |
|---|---|---|
| `frontend/androidApp/app/build.gradle.kts` | Modified | Android Studio/Gradle build alignment. |
| `frontend/androidApp/.artifacts/.../*.artifact.md` | Modified | Updated generated planning/walkthrough artifacts. |

This small commit was build-tool maintenance, not a new user feature.

### Recorded phase 4 — Authenticated Android and web application flows

**Git evidence:** `5cd821a`, “Phase 4”.

This phase transformed both clients from destination demonstrations into
navigable authenticated applications.

#### Android files

| File | Change | What it held |
|---|---|---|
| `AppContainer.kt` | Added | Manual application composition root for API, repositories, token store, and session state. |
| `MainActivity.kt` | Modified | Launched the complete `TourVerseApp` navigation tree. |
| `data/model/AuthModels.kt` | Added | Android authentication and user DTOs. |
| `data/model/CommunityModels.kt` | Added | Android review, favourite, trip, and related DTOs. |
| `data/remote/AuthApi.kt` | Added | Authentication/profile API calls. |
| `data/remote/TourismApi.kt` | Modified | Expanded authenticated/community endpoints. |
| `data/repository/AuthRepositories.kt` | Added | Authentication and profile repository operations. |
| `data/repository/CommunityRepositories.kt` | Added | Review, favourite, and trip repository operations. |
| `data/session/SessionTokenStore.kt` | Added | Token-persistence abstraction. |
| `data/session/EncryptedSessionTokenStore.kt` | Added | Encrypted Android token persistence. |
| `state/SessionManager.kt` | Added | Central access/refresh session state and recovery. |
| `ui/navigation/TourVerseApp.kt` | Added | Authenticated route graph and application navigation. |
| `ui/screens/AuthScreens.kt` | Added | Login, registration, profile, and account forms. |
| `ui/screens/AuthViewModels.kt` | Added | Authentication/profile screen state and actions. |
| `ui/screens/CommunityScreens.kt` | Added | Favourite, review, trip, and community-oriented UI. |
| `ui/screens/CommunityViewModels.kt` | Added | Community screen state and repository coordination. |
| `ui/screens/HomeScreen.kt`, `HomeViewModel.kt` | Modified | Integrated destination discovery with navigation and session-aware UI. |
| `test/.../SessionTokenStoreTest.kt` | Added | Token-store behavior without production secrets. |
| `app/build.gradle.kts` | Modified | Added navigation, lifecycle, security, and client dependencies/configuration. |

All Kotlin paths are below
`frontend/androidApp/app/src/main/kotlin/com/tourverse/`.

#### Web files

| File | Change | What it held |
|---|---|---|
| `src/RouterApp.tsx` | Added | Complete browser route tree. |
| `src/components/AppShell.tsx` | Added | Shared navigation, header, content shell, and session-aware links. |
| `src/components/StatusStates.tsx` | Added | Reusable loading, error, and empty states. |
| `src/features/auth/AuthPages.tsx` | Added | Registration and login pages. |
| `src/features/destinations/DestinationDetailsPage.tsx` | Added | Destination detail route and related actions. |
| `src/features/favorites/FavoritesPage.tsx` | Added | Authenticated favourite list. |
| `src/features/profile/ProfilePage.tsx` | Added | Profile update and account operations. |
| `src/features/trips/TripPages.tsx` | Added | Trip lists, creation, details, and destination membership. |
| `src/pages/PlaceholderPages.tsx` | Added | Honest placeholders for backend modules without complete web portals. |
| `src/routes/ProtectedRoute.tsx` | Added | Redirect/protection for authenticated pages. |
| `src/models/Auth.ts` | Added | Browser user, token, login, registration, and profile types. |
| `src/models/Community.ts` | Added | Browser favourite, review, and trip types. |
| `src/services/authApi.ts` | Added | Authentication and profile HTTP calls. |
| `src/services/communityApi.ts` | Added | Favourite, review, and trip HTTP calls. |
| `src/services/session.ts` | Added | Access token, refresh coordination, persistence, and authenticated fetch. |
| `src/services/session.test.ts` | Added | Refresh coordination and session-clearing tests. |
| `src/state/AuthContext.tsx` | Added | React authentication context and hooks. |
| `src/App.tsx`, `src/main.tsx` | Modified | Switched the entry point to routed, stateful application composition. |
| `src/components/DestinationCard.tsx` | Modified | Linked cards into destination details. |
| `src/styles/index.css` | Modified | Added navigation, forms, authenticated pages, responsive layouts, and status styling. |
| `package.json`, `package-lock.json` | Modified | Added React Router and locked the expanded dependency tree. |

#### Supporting backend and documentation files

| File | Change | What it held |
|---|---|---|
| `backend/scripts/seed-development-data.sql` | Added | Repeatable non-production categories and destination catalogue seed data. |
| `backend/src/main/kotlin/com/tourverse/routes/DestinationRoutes.kt` | Modified | Reinforced destination authorization behavior used by the clients. |
| `backend/src/test/kotlin/com/tourverse/DestinationAuthorizationTest.kt` | Added | Anonymous, user, and administrator destination route boundaries. |
| `backend/.env.example` | Modified | Documented development seed behavior and client-relevant settings. |
| Root/application READMEs and `docs/*` | Modified | Recorded authenticated client architecture and commands. |

### Recorded phase 5 — Global catalogue, provenance, and maps

**Git evidence:** `35b553b`, “Phase 5”.

This phase combined the global catalogue foundation, cache/provenance model,
provider boundaries, administrator review UI, and map presentation.

#### Database and backend catalogue files

| File | Change | What it held |
|---|---|---|
| `V13__global_destination_catalogue.sql` | Added | Country codes, import batches, candidates, source references, candidate state, and catalogue indexes. |
| `V14__destination_cache_and_provenance.sql` | Added | Origin, cache state, expiry, verification, content hashes, editorial locks, provider metadata, and field provenance. |
| `database/tables/DestinationImportTables.kt` | Added | Exposed mappings for batches, candidates, sources, and provenance. |
| `database/tables/DestinationsTable.kt` | Modified | Added country code, origin, cache, verification, and editorial fields. |
| `models/CountryModels.kt` | Added | Public country/count response models. |
| `models/DestinationImportModels.kt` | Added | Provider queries, batches, candidates, review actions, sync jobs, and Google Place DTOs. |
| `models/Destination.kt` | Modified | Added public provenance, verification, attribution, map, and provider-link fields. |
| `models/DestinationQuery.kt` | Modified | Added country-code and catalogue-aware query behavior. |
| `models/CreateDestinationRequest.kt`, `UpdateDestinationRequest.kt` | Modified | Kept administrator curation aligned with the expanded destination record. |
| `repositories/DestinationImportRepository.kt` | Added | Persistence contract for catalogue jobs, candidates, sources, and review. |
| `repositories/PostgresDestinationImportRepository.kt` | Added | PostgreSQL implementation of the import/review contract. |
| `repositories/DestinationRepository.kt` | Modified | Added country lists and catalogue-aware public reads. |
| `repositories/PostgresDestinationRepository.kt` | Modified | Filtered rejected/development data and projected provenance metadata. |

#### Provider and catalogue services

| File | Change | What it held |
|---|---|---|
| `services/DestinationImportProvider.kt` | Added | Provider-neutral discovery/detail interfaces and normalized candidate model. |
| `services/WikidataDestinationImportProvider.kt` | Added | Bounded SPARQL/entity discovery and normalized Wikidata candidates. |
| `services/OpenTripMapDestinationImportProvider.kt` | Added | Disabled-by-default OpenTripMap boundary and fixture parsing. |
| `services/GooglePlacesService.kt` | Added | Bounded minimal place search, explicit linking, and provider-not-configured handling. |
| `services/DestinationImportService.kt` | Added | Batch creation, provider execution, retries, and candidate persistence. |
| `services/DestinationCatalogueServices.kt` | Added | Freshness policy, hashing, normalization, merge precedence, refresh, and sync orchestration. |
| `services/DestinationDuplicateDetector.kt` | Added | Normalized duplicate and possible-duplicate detection. |
| `services/CountryCodeService.kt` | Added | Country-name and ISO-code normalization. |
| `services/SourceCategoryMapper.kt` | Added | Maps provider categories into TourVerse categories. |
| `services/DestinationService.kt` | Modified | Public catalogue metadata and administrative curation behavior. |
| `services/DestinationValidator.kt` | Modified | Current country, coordinate, URL, and catalogue field validation. |

#### Catalogue routes and backend wiring

| File | Change | What it held |
|---|---|---|
| `routes/DestinationImportRoutes.kt` | Added | Searches, batches, retries, candidate edits, approval, rejection, and linking. |
| `routes/DestinationCatalogueRoutes.kt` | Added | Sync, country sync, refresh, jobs, stale records, sources, and Google Place linking. |
| `routes/DestinationRoutes.kt` | Modified | Public country counts and catalogue-aware reads. |
| `plugins/Routing.kt` | Modified | Constructed and registered import/catalogue/provider services. |
| `plugins/StatusPages.kt` | Modified | Added provider-not-configured response behavior. |
| `exceptions/ApiExceptions.kt` | Modified | Added provider configuration exception. |
| `application.conf` | Modified | Kept port/runtime behavior aligned with the full system. |
| `backend/build.gradle.kts` | Modified | Added provider-side Ktor Client dependencies. |
| `backend/.env.example` | Modified | Documented provider keys and development-data visibility. |
| `backend/Dockerfile`, `backend/docker-compose.yml` | Modified | Aligned containers with port `8081` and catalogue configuration. |
| `backend/scripts/seed-development-data.sql` | Modified | Classified existing Uganda data as development seed content. |

#### Catalogue tests

| File | Change | What it verified |
|---|---|---|
| `CountryCodeServiceTest.kt` | Added | Country normalization and ISO mapping. |
| `DestinationDuplicateDetectorTest.kt` | Added | Duplicate scoring and normalization. |
| `DestinationImportAuthorizationTest.kt` | Added | Import access boundaries and fixture-backed administrator flow. |
| `DestinationCatalogueAuthorizationTest.kt` | Added | Anonymous/user rejection for synchronization. |
| `DestinationCatalogueServicesTest.kt` | Added | Freshness, hash stability, merge priority, provider validation, and missing configuration. |
| `WikidataDestinationImportProviderTest.kt` | Added | Wikidata query/detail normalization behavior. |
| `OpenTripMapDestinationImportProviderTest.kt` | Added | Offline fixture parsing. |
| `DestinationAuthorizationTest.kt`, `DestinationQueryTest.kt` | Modified | Current catalogue and authorization behavior. |

#### Web catalogue and map files

| File | Change | What it held |
|---|---|---|
| `features/admin/DestinationImportsPage.tsx` | Added | Administrator batch search and candidate review UI. |
| `routes/AdminRoute.tsx` | Added | Role-protected administrator route wrapper. |
| `models/DestinationImport.ts` | Added | TypeScript batch, candidate, source, and review models. |
| `services/destinationImportApi.ts` | Added | Authenticated import/review API calls. |
| `features/destinations/DestinationMap.tsx` | Added | Browser Google map and resilient fallbacks. |
| `features/destinations/mapUtils.ts` | Added | Coordinate validation, Maps URLs, and shared script loading. |
| `features/destinations/mapUtils.test.ts` | Added | Coordinate and Maps URL tests. |
| `features/destinations/DestinationDetailsPage.tsx` | Modified | Added map and attribution to destination details. |
| `models/Destination.ts` | Modified | Added provenance, verification, map, country, and cache-facing fields. |
| `services/api.ts`, `services/api.test.ts` | Modified | Current filters, country endpoint, and error handling. |
| `RouterApp.tsx`, `AppShell.tsx`, `App.tsx` | Modified | Added catalogue administration navigation and routing. |
| `.env.example`, `vite-env.d.ts` | Modified | Declared browser Maps configuration. |
| `styles/index.css` | Modified | Styled maps, admin review, metadata, and fallbacks. |

#### Android map and catalogue files

| File | Change | What it held |
|---|---|---|
| `ui/screens/DestinationMap.kt` | Added | Maps Compose marker, coordinate checks, and external-map fallback. |
| `test/.../DestinationMapTest.kt` | Added | Map-coordinate and URI behavior. |
| `data/model/Destination.kt`, `DestinationQuery.kt` | Modified | Current catalogue/provenance/map contract. |
| `data/remote/TourismApi.kt`, `DestinationRepository.kt` | Modified | Country, pagination, filter, and current response handling. |
| `ui/screens/HomeScreen.kt`, `HomeViewModel.kt` | Modified | Global catalogue search/filter/paging behavior. |
| `ui/screens/CommunityScreens.kt` | Modified | Used current destination data in community views. |
| `AndroidManifest.xml` | Modified | Added the Google Maps key placeholder. |
| `app/build.gradle.kts` | Modified | Added Maps Compose, key configuration, SDK alignment, and environment settings. |
| `TourismApiTest.kt` | Modified | Expanded destination-query and response coverage. |

OpenAPI, Postman, the READMEs, `API_CONTRACT.md`, `ARCHITECTURE.md`, and
`PROJECT_TREE.txt` were modified in the same phase so the catalogue and map
behavior was documented with the code.

### Recorded production-readiness phase — Secure staging preparation

**Git evidence:** `3b9fa90`, “Prepare TourVerse for secure staging
deployment”.

This phase turned the working application into a repository-ready staging
candidate.

#### Backend security and private password workflow

| File | Change | What it held |
|---|---|---|
| `models/AuthModels.kt` | Modified | Added `ChangePasswordRequest`. |
| `services/AuthService.kt` | Modified | Verified the current password, hashed the replacement, and revoked refresh sessions. |
| `services/AuthValidator.kt` | Modified | Enforced an 8–128 character password range and composition rules. |
| `routes/AuthRoutes.kt` | Modified | Added authenticated `PUT /api/users/me/password`. |
| `plugins/Security.kt` | Modified | Required strong production JWT configuration, explicit HTTPS origins, PostgreSQL, and disabled development seeds. |
| `plugins/Observability.kt` | Modified | Added production HSTS alongside existing security headers. |
| `scripts/change-password.ps1` | Added | Secure interactive local/HTTPS password-change client that keeps passwords out of shell history. |
| `test/.../AuthValidatorTest.kt` | Modified | Added excessive-password-length coverage. |
| `openapi/tourverse-openapi.yaml` | Modified | Documented the password-change contract. |

#### Android production files

| File | Change | What it held |
|---|---|---|
| `app/build.gradle.kts` | Modified | Added external signing inputs, HTTPS API validation, release shrinking, and production release gates. |
| `app/src/main/AndroidManifest.xml` | Modified | Referenced declarative network security configuration. |
| `app/src/main/res/xml/network_security_config.xml` | Added | Production/default policy rejecting cleartext traffic. |
| `app/src/development/res/xml/network_security_config.xml` | Added | Local development HTTP override. |
| `app/src/emulator/res/xml/network_security_config.xml` | Added | Emulator HTTP override. |
| `app/src/physical/res/xml/network_security_config.xml` | Added | LAN physical-device HTTP override. |

#### CI and deployment files

| File | Change | What it held |
|---|---|---|
| `.github/workflows/ci.yml` | Added | Backend, web, and Android jobs with dependency caching and read-only permissions. |
| `backend/Dockerfile` | Modified | Multi-stage build, non-root runtime, curl-based health check, and production environment. |
| `backend/docker-compose.yml` | Modified | Catalogue/provider environment variables and production seed protection. |
| `frontend/webApp/Dockerfile` | Added | Node production build followed by an Nginx runtime image. |
| `frontend/webApp/nginx.conf` | Added | SPA fallback, immutable asset caching, and browser security headers. |
| `deploy/Caddyfile` | Added | HTTPS proxy routing `/api/*` to Ktor and other paths to the web container. |
| `deploy/docker-compose.production.yml` | Added | PostgreSQL, API, web, Caddy, health checks, volumes, restart rules, and private variables. |
| `deploy/production.env.example` | Added | Placeholder-only production environment template. |
| `deploy/README.md` | Added | Staging, DNS, secrets, backup, update, rollback, and launch checklist. |
| `.gitignore` | Modified | Added keystores, Kotlin build state, and production-local secret exclusions. |

#### Documentation and API-tool files

| File | Change | What it held |
|---|---|---|
| `README.md` | Modified | Linked the production runbook. |
| `backend/README.md` | Modified | Added password change and deployment/security behavior. |
| `frontend/androidApp/README.md` | Modified | Documented HTTPS flavours and private release signing. |
| `frontend/webApp/README.md` | Modified | Documented the production container and same-origin API. |
| `docs/API_CONTRACT.md` | Modified | Added the password-change endpoint. |
| `docs/ARCHITECTURE.md` | Modified | Added V14 and production topology. |
| `docs/PROJECT_TREE.txt` | Modified | Added production-readiness files. |
| `postman/specs/TourVerse API/openapi.yaml` | Modified | Mirrored the current password API contract. |
| `postman/collections/TourVerse API/.resources/definition.yaml` | Modified | Replaced a concrete bearer token with the `accessToken` collection variable. |

### Current documentation phase — Creation journal

This phase is the documentation work represented by the current file.

| File | Change | What it holds |
|---|---|---|
| `docs/TourVerse creation journal.md` | Added, then expanded | End-to-end development narrative, definitions of the technologies used, architecture decisions, problems and lessons, current status, future direction, and this phase-by-phase file responsibility record. |

No backend, Android, web, database, test, deployment, or runtime behavior is
changed by this journal phase.

### Current repository-organization phase — Consolidated frontend directory

This phase grouped the two client projects beneath one repository boundary
without combining their source code or build systems.

| File or path | Change | What it holds |
|---|---|---|
| `frontend/androidApp/` | Moved from root `androidApp/` | Complete native Android project, preserving its Gradle wrapper, sources, tests, resources, package name, application ID, and README. |
| `frontend/webApp/` | Moved from root `webApp/` | Complete React/TypeScript project, preserving npm configuration, sources, tests, Dockerfile, Nginx configuration, and README. |
| `frontend/README.md` | Added | Frontend boundary, client responsibilities, commands, and links to both application guides. |
| `.github/workflows/ci.yml` | Modified | Uses `frontend/androidApp` and `frontend/webApp` as job working directories and dependency-cache paths. |
| `deploy/docker-compose.production.yml` | Modified | Builds the web image from `../frontend/webApp`. |
| `.gitignore` | Modified | Ignores the web environment file at its new location. |
| `.postman/resources.yaml` | Modified | Points Postman workspace metadata to the moved web package file. |
| `README.md` | Modified | Shows the nested frontend layout and updated startup/build commands. |
| `frontend/androidApp/README.md` | Modified | Uses the new Android project path in setup instructions. |
| `frontend/webApp/README.md` | Modified | Uses the new web project path and source-tree heading. |
| `docs/PROJECT_TREE.txt` | Modified | Prefixes Android and web source paths with `frontend/`. |
| `docs/TourVerse creation journal.md` | Modified | Documents the new structure while preserving the historical path explanation. |
| `frontend/androidApp/.artifacts/.../*.artifact.md` | Modified | Updates historical local file links so they still open the moved Gradle file. |

Backend source, database migrations, REST behavior, client package names, API
URLs, and user-facing behavior were not changed by this organizational phase.

### Current documentation phase — Source-code explanations

This phase added concise learning-oriented comments to the production source.
The comments explain the responsibility of functions, classes, interfaces,
React components, Compose screens, ViewModels, repositories, services, route
groups, serializers, validators, and important conversion helpers. They do not
change runtime behavior.

| Files worked on | Change | What was explained |
|---|---|---|
| `backend/src/main/kotlin/com/tourverse/Application.kt` and `plugins/*.kt` | Commented | Backend startup and the Ktor plugin pipeline for HTTP, logging, security, serialization, routing, and error handling. |
| `backend/src/main/kotlin/com/tourverse/routes/*.kt` | Commented | Endpoint registration, request parsing, authentication boundaries, and service delegation. |
| `backend/src/main/kotlin/com/tourverse/services/*.kt` | Commented | Authentication, profiles, destinations, categories, community features, catalogue synchronization, provider imports, validation, mapping, and duplicate detection. |
| `backend/src/main/kotlin/com/tourverse/repositories/*.kt` | Commented | Repository contracts, PostgreSQL queries, transactions, persistence mapping, and import-candidate storage. |
| `backend/src/main/kotlin/com/tourverse/database/**/*.kt` | Commented | Database initialization and Exposed table responsibilities. |
| `backend/src/main/kotlin/com/tourverse/models/*.kt`, `dto/*.kt`, `security/*.kt`, `utils/*.kt`, and `exceptions/*.kt` | Commented | API models, shared response shapes, token/password operations, serializers, environment helpers, and error types. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/MainActivity.kt`, `AppContainer.kt`, and `ui/navigation/*.kt` | Commented | Android startup, dependency wiring, session-aware navigation, and top-level screen flow. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/ui/**/*.kt` | Commented | Compose screens, reusable UI sections, maps, ViewModel state, events, and asynchronous loading. |
| `frontend/androidApp/app/src/main/kotlin/com/tourverse/data/**/*.kt` and `state/*.kt` | Commented | API contracts, client setup, models, repositories, encrypted token storage, and session management. |
| `frontend/webApp/src/App.tsx`, `RouterApp.tsx`, `main.tsx`, `components/*.tsx`, and `routes/*.tsx` | Commented | React startup, destination browsing, shared layout, reusable status components, and route protection. |
| `frontend/webApp/src/features/**/*.tsx` and `pages/*.tsx` | Commented | Authentication, profiles, favourites, trips, destination details/maps, administration, event handlers, and screen state. |
| `frontend/webApp/src/services/*.ts`, `state/*.tsx`, and `models/*.ts` | Commented | HTTP requests, session persistence, authentication context, conversion helpers, and TypeScript data contracts. |

No tests, migrations, API contracts, configuration values, or application
logic were changed by this documentation phase.

### How to use the file journal during future development

When a new TourVerse phase is completed, add:

1. The phase objective.
2. The Git commit or pull-request reference after it exists.
3. Every added, modified, replaced, or deleted source/configuration file.
4. What responsibility each file gained or lost.
5. The migration number if the database changed.
6. The tests proving the behavior.
7. Documentation, environment, and deployment changes.
8. Any intentionally deferred work.

This keeps the journal useful as both a learning record and a technical
handover document.

---

## Closing reflection

TourVerse was not created as one large operation. It evolved through a sequence
of foundations:

1. Establish the database and backend.
2. Add secure identity and protected resources.
3. Build the tourism and community workflows.
4. Validate the API and persistence.
5. Build Android and web clients.
6. Repair their contract when the backend evolved.
7. Grow destination discovery without sacrificing editorial control.
8. Add provenance, freshness, maps, and provider boundaries.
9. Strengthen tests and security.
10. Prepare CI, signing, containers, HTTPS, backups, and deployment guidance.

The result is a broadly functional full-stack tourism platform with a strong
backend and a clear route to staging. The next milestone is not to redesign the
system. It is to choose the production infrastructure, deploy a controlled
staging environment, complete remaining user-facing portals, populate curated
data, and verify real-world operation before opening TourVerse to public users.
