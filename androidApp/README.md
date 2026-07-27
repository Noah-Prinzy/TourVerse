# Android application

Open the `androidApp` folder in Android Studio.

## API environments

Select the matching build variant in Android Studio:

| Flavor | API URL | Use case |
| --- | --- | --- |
| `development` | `http://127.0.0.1:8080/` | Local backend through `adb reverse` |
| `emulator` | `http://10.0.2.2:8080/` | Android emulator connecting directly to the host |
| `physical` | Configurable LAN URL | Physical phone on the same network |
| `production` | Required external value | Deployed HTTPS backend |

For the `development` flavor, expose the host backend to the connected device:

```powershell
adb reverse tcp:8080 tcp:8080
```

The physical-phone flavor defaults to `http://192.168.0.150:8080/`. Override it
without editing source:

```powershell
.\gradlew.bat :app:installPhysicalDebug `
  -Ptourverse.physicalApiUrl=http://192.168.1.25:8080/
```

Supply the deployed HTTPS URL when building production:

```powershell
.\gradlew.bat :app:assembleProductionRelease `
  -Ptourverse.productionApiUrl=https://api.your-domain.example/
```

The equivalent environment variables are `TOURVERSE_PHYSICAL_API_URL` and
`TOURVERSE_PRODUCTION_API_URL`.

Start the Ktor backend before running the app.
