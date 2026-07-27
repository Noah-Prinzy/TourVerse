# Walkthrough - Fix JVM Target Inconsistency

I have aligned the JVM targets for both Java and Kotlin compilers to version 21 in the `:app` module. This resolves the `Inconsistent JVM-target compatibility` error that was occurring during the build.

## Changes

### [app]

#### [build.gradle.kts](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/androidApp/app/build.gradle.kts)

I added the following configurations to ensure consistency:

```kotlin
android {
    // ...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}
```

## Verification Results

### Automated Tests
- Executed `gradlew :app:assembleDebug`.
- **Result:** Build finished successfully.

> [!NOTE]
> Setting both targets to Java 21 is recommended when using modern Android Studio versions and Gradle 8.0+, especially if your environment is already using a Java 21 daemon as seen in your `gradlew -v` output.
