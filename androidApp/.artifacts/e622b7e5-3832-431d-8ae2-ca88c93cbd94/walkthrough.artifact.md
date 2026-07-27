# Walkthrough - Build and Connectivity Fixes

I have resolved the JVM target mismatch and fixed the networking configuration to allow the physical device to connect to your local backend.

## Changes

### [app]

#### [build.gradle.kts](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/androidApp/app/build.gradle.kts)
Aligned JVM targets to version 21.

#### [TourismApi.kt](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/androidApp/app/src/main/kotlin/com/tourverse/data/remote/TourismApi.kt)
Updated the API base URL to use your computer's local IP address (`192.168.0.150`).

```kotlin
    companion object {
        private const val BASE_URL = "http://192.168.0.150:8080/"
    }
```

## Verification Results

### Build and Deployment
- Executed `gradlew :app:assembleDebug`.
- Successfully deployed to physical device **Z2577** via Wireless ADB.

### Connectivity Checklist
> [!IMPORTANT]
> Since the app is now deployed with the new IP, please ensure:
> 1. **Same Wi-Fi**: Phone and PC are on the same network.
> 2. **Backend**: Running on port 8080.
> 3. **Host**: Listening on `0.0.0.0`.
> 4. **Firewall**: Port 8080 is open for inbound traffic on Windows.
