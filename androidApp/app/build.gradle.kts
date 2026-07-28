plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val physicalApiBaseUrl = providers
    .gradleProperty("tourverse.physicalApiUrl")
    .orElse(providers.environmentVariable("TOURVERSE_PHYSICAL_API_URL"))
    .orElse("http://192.168.1.73:8080/")

val productionApiBaseUrl = providers
    .gradleProperty("tourverse.productionApiUrl")
    .orElse(providers.environmentVariable("TOURVERSE_PRODUCTION_API_URL"))
    .orElse("")

android {
    namespace = "com.tourverse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tourverse"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "apiEnvironment"

    productFlavors {
        create("development") {
            dimension = "apiEnvironment"
            applicationIdSuffix = ".development"
            versionNameSuffix = "-development"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "http://127.0.0.1:8080/".asBuildConfigString()
            )
        }

        create("emulator") {
            dimension = "apiEnvironment"
            applicationIdSuffix = ".emulator"
            versionNameSuffix = "-emulator"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "http://10.0.2.2:8080/".asBuildConfigString()
            )
        }

        create("physical") {
            dimension = "apiEnvironment"
            applicationIdSuffix = ".physical"
            versionNameSuffix = "-physical"
            buildConfigField(
                "String",
                "API_BASE_URL",
                physicalApiBaseUrl.get().asBuildConfigString()
            )
        }

        create("production") {
            dimension = "apiEnvironment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                productionApiBaseUrl.get().asBuildConfigString()
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-okhttp:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")

    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-client-mock:3.1.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
