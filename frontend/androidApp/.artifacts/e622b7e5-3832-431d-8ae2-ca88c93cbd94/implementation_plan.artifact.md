# Fix TourVerse Connection Error

The "Unable to connect" error is caused by your computer's local IP address changing. The app is currently trying to connect to `192.168.0.150`, but your computer's new IP is `192.168.1.73`.

## Proposed Changes

### [app]

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/frontend/androidApp/app/build.gradle.kts)
- Update the default value for `physicalApiBaseUrl` to use the current IP: `http://192.168.1.73:8080/`.

## Verification Plan

### Manual Verification
1. Re-sync Gradle to apply the IP change to the `physical` flavor.
2. Deploy the `physicalDebug` variant to your phone.
3. Verify that the "Unable to connect" message is gone and destinations load correctly.

---

> [!TIP]
> **Pro-Tip: Avoid editing this file every time your IP changes!**
> You can set this IP in your `gradle.properties` file (which is usually ignored by Git) instead of hardcoding it in the build script:
> ```properties
> tourverse.physicalApiUrl=http://192.168.1.73:8080/
> ```
> The build script is already set up to look for this property!
