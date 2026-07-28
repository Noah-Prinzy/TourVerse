# TourVerse Android Application

This is the authoritative guide to the Android client currently implemented in
`androidApp`.

## Current implementation

The Android application is a routed Jetpack Compose client.
It currently:

- Starts from `MainActivity` and renders a Navigation Compose application
- Consumes the backend's paginated UUID destination response
- Supports search, country/city/category filters, sorting, and pagination
- Uses a ViewModel and `StateFlow` for loading, empty, success, and error states
- Displays destination cards in a lazy list
- Loads remote images with Coil and shows a local null-image fallback
- Provides a retry button after an API failure
- Preserves backend validation messages and maps connection failures to a stable
  user-facing message
- Supports separate API URLs for local ADB, emulator, physical-device, and
  production builds
- Provides registration, login, refresh rotation, protected navigation,
  profiles, logout, and account deletion
- Encrypts the persisted refresh token with Android Keystore AES-GCM
- Provides destination details, backend categories, favorites, review CRUD,
  and private trip CRUD with destination add/remove

## Technology and requirements

| Area | Implementation |
| --- | --- |
| Language | Kotlin 2.1.20 |
| UI | Jetpack Compose and Material 3 |
| Android Gradle Plugin | 8.13.2 |
| Compile/target SDK | 35 |
| Minimum SDK | 26 |
| Java/Kotlin target | 21 |
| HTTP | Ktor Client 3.1.2 with OkHttp |
| JSON | Kotlin serialization |
| Images | Coil 3.2.0 |
| State | Android ViewModel, coroutines, and `StateFlow` |
| Tests | Kotlin/JUnit and Ktor MockEngine |

Use Android Studio with JDK 21 and an installed Android SDK 35.

## Architecture

```text
MainActivity
    |
    v
HomeScreen <-- observes -- HomeViewModel / HomeUiState
                              |
                              v
                    DestinationRepository
                              |
                              v
                         TourismApi
                              |
                              v
                    Ktor/OkHttp HttpClient
                              |
                              v
                  GET /api/destinations
```

Important files:

```text
app/src/main/kotlin/com/tourverse/
|-- MainActivity.kt
|-- data/
|   |-- model/Destination.kt
|   |-- remote/ApiClient.kt
|   |-- remote/TourismApi.kt
|   `-- repository/DestinationRepository.kt
`-- ui/
    |-- screens/HomeScreen.kt
    |-- screens/HomeViewModel.kt
    `-- theme/Theme.kt
```

`ApiClient` enables tolerant JSON parsing with `ignoreUnknownKeys` and
`isLenient`. `TourismApi` normalizes the generated base URL so it ends in `/`
and refuses to run when a production URL is missing.

## API build environments

`app/build.gradle.kts` defines an `apiEnvironment` flavor dimension:

| Flavor | Generated `BuildConfig.API_BASE_URL` | Use |
| --- | --- | --- |
| `development` | `http://127.0.0.1:8080/` | Device connected through `adb reverse` |
| `emulator` | `http://10.0.2.2:8080/` | Android emulator reaching the host |
| `physical` | Configurable; currently defaults to a LAN URL | Phone and backend on the same LAN |
| `production` | Required external value; defaults to empty | Deployed HTTPS API |

Development, emulator, and physical flavors add application ID suffixes, so
they can coexist on a device.

### Connected device through ADB

Start the backend, connect and authorize the device, then run:

```powershell
adb reverse tcp:8080 tcp:8080
adb reverse --list
.\gradlew.bat :app:installDevelopmentDebug
```

The development build calls `127.0.0.1:8080` on the device; the reverse-port
rule forwards that traffic to the computer.

### Android emulator

The emulator exposes the host computer as `10.0.2.2`:

```powershell
.\gradlew.bat :app:installEmulatorDebug
```

No `adb reverse` rule is required for this flavor.

### Physical device over Wi-Fi

The phone and computer must be on the same network. Confirm the computer's
current LAN address, ensure the backend listens on an accessible interface,
and allow the port through the firewall. Supply the current address at build
time:

```powershell
.\gradlew.bat :app:installPhysicalDebug `
  "-Ptourverse.physicalApiUrl=http://192.168.1.25:8080/"
```

The equivalent environment variable is:

```text
TOURVERSE_PHYSICAL_API_URL
```

Do not rely permanently on the source default because LAN addresses can change.

### Production

The production flavor intentionally has no repository-stored server address:

```powershell
.\gradlew.bat :app:assembleProductionRelease `
  "-Ptourverse.productionApiUrl=https://api.example.com/"
```

The equivalent environment variable is:

```text
TOURVERSE_PRODUCTION_API_URL
```

Use an HTTPS URL. A production build with an empty value compiles, but
`TourismApi` fails at runtime when instantiated.

## Build and run

Open `androidApp` as the project in Android Studio and select the needed build
variant, or use PowerShell:

```powershell
.\gradlew.bat :app:assembleDevelopmentDebug
.\gradlew.bat :app:assembleEmulatorDebug
.\gradlew.bat :app:assemblePhysicalDebug
```

For complete client verification:

```powershell
.\gradlew.bat clean test assembleDevelopmentDebug assembleEmulatorDebug assemblePhysicalDebug --no-daemon
```

Focused unit tests cover pagination validation, location formatting, safe query
serialization, paginated response parsing, backend messages, and malformed
error fallbacks.

## UI behavior

`HomeViewModel` loads immediately and retains the active query and response
metadata:

- Loading shows a centered progress indicator.
- Search uses a short debounce and supersedes stale requests.
- Filters, sorting, and page-size changes reset to page 1.
- Previous and next actions respect the backend's pagination metadata.
- Success displays the TourVerse heading and one card per destination.
- Empty responses display an explicit no-results state.
- Failure displays the exception message and a `Try again` action.

Cards render the cover image or fallback, category, name, derived city/country
location, and description. Rating was removed because it is not present in the
backend destination response.

The manifest grants internet access and currently permits cleartext HTTP to
support local development. Production security should disable unrestricted
cleartext traffic, normally through build-specific manifest configuration.

## Backend destination contract

The Android DTO now matches the backend's string UUID, country, nullable city,
nullable coordinates, nullable cover URL, and timestamp fields.
`TourismApi.getDestinations` returns `PagedDestinationResponse` and safely adds
the backend's search, filter, page, size, and sort parameters through Ktor.

Non-success responses are decoded as `ApiMessage` when possible. Empty or
malformed error bodies use a stable HTTP-status fallback. Retry repeats the
active query.

## Known limitations and next work

- Uses a manual application composition root rather than a DI framework.
- No accessibility-specific semantics beyond basic content descriptions.
- No local database, cache, or offline mode.
- No instrumentation/UI tests; current tests focus on models and API behavior.
- API base URLs are build-time values rather than runtime settings.
- Services, bookings, notifications, and full role-specific portals are not yet
  exposed.
