# TourVerse

TourVerse is a tourism-platform repository with a Kotlin/Ktor/PostgreSQL
backend, a Jetpack Compose Android client, and a React/TypeScript web client.

The backend currently contains the broad platform feature set. The Android and
web clients are early destination-listing interfaces and do not yet expose most
backend modules.

## Repository status

| Area | Implemented state |
| --- | --- |
| Backend | Destinations, authentication, profiles, categories, reviews, favorites, trips, tourism services, bookings, notifications, administration, migrations, API hardening, tests, and deployment files |
| Android | One Compose destination browser with paginated search/filter/sort state and four configurable API environments |
| Web | One responsive React destination browser with paginated search/filter/sort state |
| Documentation | Architecture, API contract, project tree, and one authoritative README inside each application folder |

Both clients now model the backend's UUID-based paginated destination contract,
including nullable destination fields, query parameters, pagination metadata,
and standard API errors. Live browser/device verification remains distinct from
successful client builds and unit tests.

## Project layout

```text
TourVerse/
|-- androidApp/   Android application and its authoritative README
|-- backend/      Ktor API, PostgreSQL migrations, tests, and README
|-- docs/         Cross-project architecture, API contract, and project tree
|-- webApp/       React application and its authoritative README
|-- .gitignore
`-- README.md
```

Detailed guides:

- [Backend guide](backend/README.md)
- [Android guide](androidApp/README.md)
- [Web guide](webApp/README.md)
- [Architecture](docs/ARCHITECTURE.md)
- [API contract](docs/API_CONTRACT.md)
- [Project tree](docs/PROJECT_TREE.txt)

## Architecture

```text
Android client ----\
                    +--> Ktor REST API --> Services/Repositories --> PostgreSQL
Web client --------/
```

The clients call HTTP/JSON endpoints. Ktor routes delegate to services, which
perform validation and use Exposed/PostgreSQL. Flyway applies the database
schema at backend startup.

## Quick start

### 1. Start PostgreSQL

Create a local database and role. Their URL, username, and password must match
the backend's local `.env`.

### 2. Configure and run the backend

```powershell
Set-Location backend
Copy-Item .env.example .env
```

Edit `.env` locally with real database credentials and a development JWT
secret. Never commit it.

```powershell
.\gradlew.bat clean build --no-daemon
.\gradlew.bat run
```

Default backend URLs:

```text
API:      http://localhost:8080
Health:   http://localhost:8080/api/health
Docs:     http://localhost:8080/api/docs
OpenAPI:  http://localhost:8080/api/openapi.yaml
```

If Flyway reports a checksum mismatch, an applied migration differs from the
local file. Preserve shared data and investigate the migration history rather
than automatically repairing it.

### 3. Run the web application

In another terminal:

```powershell
Set-Location webApp
npm.cmd install
npm.cmd run dev
```

Vite runs at `http://localhost:5173`. Set `VITE_API_BASE_URL` in a local
untracked `.env` to override `http://localhost:8080`.

### 4. Run the Android application

Open `androidApp` in Android Studio or select a variant from PowerShell:

| Variant | Backend address |
| --- | --- |
| `developmentDebug` | `127.0.0.1:8080` through `adb reverse` |
| `emulatorDebug` | `10.0.2.2:8080` |
| `physicalDebug` | Build-time LAN URL |
| `productionRelease` | Build-time HTTPS URL |

Connected device:

```powershell
Set-Location androidApp
adb reverse tcp:8080 tcp:8080
.\gradlew.bat :app:installDevelopmentDebug
```

Emulator:

```powershell
.\gradlew.bat :app:installEmulatorDebug
```

Physical device on the same network:

```powershell
.\gradlew.bat :app:installPhysicalDebug `
  "-Ptourverse.physicalApiUrl=http://192.168.1.25:8080/"
```

Production:

```powershell
.\gradlew.bat :app:assembleProductionRelease `
  "-Ptourverse.productionApiUrl=https://api.example.com/"
```

## Verification

Backend:

```powershell
Set-Location backend
.\gradlew.bat clean build --no-daemon
```

Android:

```powershell
Set-Location androidApp
.\gradlew.bat :app:assembleDevelopmentDebug
.\gradlew.bat :app:assembleEmulatorDebug
.\gradlew.bat :app:assemblePhysicalDebug
```

Web:

```powershell
Set-Location webApp
npm.cmd run build
```

The Android and web projects currently have no automated test suites. The web
project also has no lint script.

## Security and configuration

- Real `.env` files, credentials, JWT secrets, and production URLs must remain
  outside version control.
- Browser `VITE_` variables are public build inputs and must not contain
  secrets.
- Production backend startup requires a strong JWT secret and explicit CORS
  origins.
- Local Android variants allow HTTP; deployed traffic should use HTTPS.
- Rotate any password or secret that has been committed, logged, or shared.

## Implemented boundaries

The backend stores image URLs and payment status but does not upload image
files or process real payments. Email delivery, push notifications, maps
provider integration, production hosting, monitoring, and backup operations
require external services and operational configuration.

The next frontend priorities are live browser/device validation, authentication,
and incremental UI coverage for the backend's remaining platform modules.
