# Walkthrough - Fixed Connection Error

I have updated the IP address used by the `physical` build flavor to match your computer's current local network address.

## Changes

### [app]

#### [build.gradle.kts](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/frontend/androidApp/app/build.gradle.kts)
Updated the `physicalApiBaseUrl` from `192.168.0.150` to `192.168.1.73`.

## Verification Results

### Deployment
- Successfully deployed `physicalDebug` to device **Z2577**.
- Verified the backend is running and listening on port 8080.

> [!NOTE]
> Since your IP changed from `192.168.0.x` to `192.168.1.x`, it's possible your router assigned a new subnet. If you move between different Wi-Fi networks, you'll likely need to perform this update again.
