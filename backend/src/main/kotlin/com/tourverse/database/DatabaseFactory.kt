package com.tourverse.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.tourverse.utils.AppEnvironment
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.jetbrains.exposed.v1.jdbc.Database
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object DatabaseFactory {

    private const val DATABASE_URL = "TOURVERSE_DATABASE_URL"
    private const val DATABASE_USER = "TOURVERSE_DATABASE_USER"
    private const val DATABASE_PASSWORD = "TOURVERSE_DATABASE_PASSWORD"
    private const val MARKETPLACE_DATABASE_URL = "DATABASE_URL"

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    @Volatile
    private var dataSource: HikariDataSource? = null

    @Synchronized
    fun init() {
        if (dataSource != null) {
            logger.info("TourVerse database is already initialized.")
            return
        }

        logger.info("Initializing TourVerse database connection pool.")
        val databaseConfig = loadDatabaseConfig()
        val newDataSource = createDataSource(databaseConfig)

        try {
            runMigrations(newDataSource)

            Database.connect(newDataSource)
            logger.info("Exposed connected successfully to the TourVerse database.")

            dataSource = newDataSource

            logger.info("TourVerse database initialization completed successfully.")
        } catch (exception: Exception) {
            newDataSource.close()

            throw IllegalStateException(
                "TourVerse database initialization failed. " +
                        "Check the database connection and migration configuration.",
                exception
            )
        }
    }

    private fun loadDatabaseConfig(): DatabaseConfig {
        val jdbcUrl = AppEnvironment.get(DATABASE_URL)
        val username = AppEnvironment.get(DATABASE_USER)
        val password = AppEnvironment.get(DATABASE_PASSWORD)

        if (jdbcUrl != null || username != null || password != null) {
            return DatabaseConfig(
                jdbcUrl = requireNotNull(jdbcUrl) { "Missing required database configuration: $DATABASE_URL." },
                username = requireNotNull(username) { "Missing required database configuration: $DATABASE_USER." },
                password = requireNotNull(password) { "Missing required database configuration: $DATABASE_PASSWORD." }
            )
        }

        val marketplaceUrl = AppEnvironment.get(MARKETPLACE_DATABASE_URL)
            ?: throw IllegalStateException(
                "Missing database configuration. Set the TOURVERSE_DATABASE_* variables " +
                    "or provide the Vercel Marketplace DATABASE_URL."
            )

        return parseMarketplaceDatabaseUrl(marketplaceUrl)
    }

    private fun parseMarketplaceDatabaseUrl(value: String): DatabaseConfig {
        val uri = runCatching { URI(value) }.getOrElse {
            throw IllegalStateException("DATABASE_URL is not a valid PostgreSQL URL.", it)
        }
        require(uri.scheme == "postgres" || uri.scheme == "postgresql") {
            "DATABASE_URL must use the postgres or postgresql scheme."
        }

        val userInfo = uri.rawUserInfo?.split(':', limit = 2)
            ?: throw IllegalStateException("DATABASE_URL must include a database user and password.")
        require(userInfo.size == 2) {
            "DATABASE_URL must include a database user and password."
        }

        val host = uri.host ?: throw IllegalStateException("DATABASE_URL must include a database host.")
        val port = if (uri.port == -1) 5432 else uri.port
        val databasePath = uri.rawPath?.takeIf { it.length > 1 }
            ?: throw IllegalStateException("DATABASE_URL must include a database name.")
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        val jdbcHost = if (host.contains(':')) "[$host]" else host

        return DatabaseConfig(
            jdbcUrl = "jdbc:postgresql://$jdbcHost:$port$databasePath$query",
            username = decodeUserInfo(userInfo[0]),
            password = decodeUserInfo(userInfo[1])
        )
    }

    private fun decodeUserInfo(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8)

    private fun createDataSource(databaseConfig: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            poolName = "TourVersePool"
            jdbcUrl = databaseConfig.jdbcUrl
            username = databaseConfig.username
            password = databaseConfig.password
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 10_000
            validationTimeout = 5_000
        }

        return try {
            HikariDataSource(hikariConfig)
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Unable to create the TourVerse database connection pool. Verify the database URL, user, and password.",
                exception
            )
        }
    }

    private fun runMigrations(dataSource: HikariDataSource) {
        logger.info("Running Flyway database migrations.")

        val migrationResult = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        logger.info(
            "Flyway migrations completed successfully. {} migration(s) applied.",
            migrationResult.migrationsExecuted
        )
    }

    private data class DatabaseConfig(
        val jdbcUrl: String,
        val username: String,
        val password: String
    )
}
