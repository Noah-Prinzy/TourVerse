plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    application
}

group = "com.tourverse"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.1.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.2")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.1.2")
    implementation("io.ktor:ktor-server-cors-jvm:3.1.2")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.1.2")
    implementation("io.ktor:ktor-server-compression-jvm:3.1.2")
    implementation("io.ktor:ktor-server-default-headers-jvm:3.1.2")
    implementation("io.ktor:ktor-server-forwarded-header-jvm:3.1.2")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.postgresql:postgresql:42.7.11")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-core:12.11.0")
    implementation("org.flywaydb:flyway-database-postgresql:12.11.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("org.jetbrains.exposed:exposed-core:1.3.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.1")
    implementation("org.jetbrains.exposed:exposed-java-time:1.3.1")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.1.2")
}

kotlin {
    jvmToolchain(17)
}
