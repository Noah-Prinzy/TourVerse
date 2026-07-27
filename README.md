# TourVerse

TourVerse is a full-stack tourism application with:

- Android: Kotlin and Jetpack Compose
- Web: TypeScript and React
- Backend: Kotlin and Ktor
- Database target: PostgreSQL
- API style: REST with JSON

Both frontend applications use the same backend and endpoint contracts.

## Project structure

```text
TourVerse/
|-- androidApp/
|-- backend/
|-- docs/
|-- webApp/
|-- .gitignore
`-- README.md
```

## Architecture

```text
Android application ----\
                         >---- Ktor REST API ---- PostgreSQL
Web application --------/
```

The backend currently separates routes, services, and repositories.

## API endpoints

```http
GET /api/health
GET /api/destinations
GET /api/destinations/{id}
```

## Start the backend

Port `8080` must be available.

```powershell
cd backend
.\gradlew.bat run
```

The backend is available at `http://localhost:8080`.

## Start the web application

In a second terminal:

```powershell
cd webApp
npm install
npm run dev
```

The development server is available at `http://localhost:5173`.
Set `VITE_API_BASE_URL` to override its default backend URL.

## Android API environments

Open `androidApp` in Android Studio and select the appropriate variant under
**Build > Select Build Variant**.

| Build variant | API URL | Intended use |
| --- | --- | --- |
| `developmentDebug` | `http://127.0.0.1:8080/` | USB or wireless ADB with port reverse |
| `emulatorDebug` | `http://10.0.2.2:8080/` | Android emulator connecting to the host |
| `physicalDebug` | Configurable LAN URL | Phone and computer on the same network |
| `productionRelease` | Externally supplied HTTPS URL | Deployed backend |

### Development device through ADB

Create and verify the reverse-port rule:

```powershell
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

Then select `developmentDebug` and run the app.

If `adb` is not on `PATH`, use the executable under the Android SDK's
`platform-tools` directory.

### Physical phone through Wi-Fi

The physical flavor currently defaults to `http://192.168.0.150:8080/`.
Confirm the computer's current address with `ipconfig`, then install with an
explicit URL:

```powershell
cd androidApp
.\gradlew.bat :app:installPhysicalDebug `
  "-Ptourverse.physicalApiUrl=http://192.168.0.150:8080/"
```

The phone and computer must be on the same network, the backend must be
running, and the firewall must allow the connection.

### Production

No real production URL is stored in the repository. Supply it at build time:

```powershell
cd androidApp
.\gradlew.bat :app:assembleProductionRelease `
  "-Ptourverse.productionApiUrl=https://api.your-domain.example/"
```

You can alternatively use:

- `TOURVERSE_PHYSICAL_API_URL`
- `TOURVERSE_PRODUCTION_API_URL`

Do not commit private configuration, `.env` files, `local.properties`, or a
real production URL.

## Android verification

Build the main development variants:

```powershell
cd androidApp
.\gradlew.bat :app:assembleDevelopmentDebug
.\gradlew.bat :app:assembleEmulatorDebug
.\gradlew.bat :app:assemblePhysicalDebug
```

## Next recommended features

1. PostgreSQL integration
2. User registration and JWT authentication
3. Destination administration
4. Favourites
5. Reviews and ratings
6. Trip itineraries
7. Maps and location
8. Image uploads
9. Weather integration
