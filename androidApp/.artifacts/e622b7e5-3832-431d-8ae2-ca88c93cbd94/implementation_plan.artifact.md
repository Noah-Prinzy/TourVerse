# Fix JVM Target Inconsistency

The build is failing due to a mismatch between the Java compiler target (1.8) and the Kotlin compiler target (21). This implementation plan addresses the issue by explicitly setting both targets to Java 21 in the `:app` module.

## Proposed Changes

### [app]

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/androidApp/app/build.gradle.kts)

- Add `compileOptions` block to set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_21`.
- Add `kotlinOptions` block (or use `compilerOptions` if using newer Kotlin Gradle Plugin features) to set `jvmTarget` to `"21"`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the build completes successfully without the JVM target compatibility error.
