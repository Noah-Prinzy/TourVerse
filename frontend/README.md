# TourVerse Frontend

The `frontend` directory contains TourVerse's two user-facing client
applications. They share the backend REST contract but remain independent
build projects:

```text
frontend/
|-- androidApp/   Native Kotlin and Jetpack Compose application
`-- webApp/       React and TypeScript browser application
```

The applications are grouped here to make the repository boundary clear:
`backend` owns persistence and business rules, while `frontend` owns the
Android and browser experiences.

## Android client

Open `frontend/androidApp` in Android Studio, or build it from PowerShell:

```powershell
Set-Location frontend/androidApp
.\gradlew.bat test assembleDevelopmentDebug
```

See [the Android guide](androidApp/README.md) for emulator, physical-device,
production URL, Maps, and release-signing configuration.

## Web client

Run the browser application with:

```powershell
Set-Location frontend/webApp
npm.cmd install
npm.cmd run dev
```

See [the web guide](webApp/README.md) for environment variables, testing,
production builds, and Docker behavior.

## Shared contract

Both clients consume the Ktor API and model destination UUIDs as strings. They
support the backend's paginated destination responses, nullable fields,
filters, sorting, timestamps, and standard API errors.

Client code must not call Wikidata, OpenTripMap, or Google Places directly.
Provider discovery and keys remain behind the backend's administrative
catalogue workflows.
