package com.tourverse.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private const val DATABASE_URL = "TOURVERSE_DATABASE_URL"
    private const val DATABASE_USER = "TOURVERSE_DATABASE_USER"
    private const val DATABASE_PASSWORD = "TOURVERSE_DATABASE_PASSWORD"

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
            dataSource = newDataSource
            logger.info("TourVerse database initialization completed successfully.")
        } catch (exception: Exception) {
            newDataSource.close()
            throw IllegalStateException(
                "TourVerse database initialization failed. Check the database connection and migration configuration.",
                exception
            )
        }
    }

    private fun loadDatabaseConfig(): DatabaseConfig {
        val environment = Dotenv.configure()
            .ignoreIfMissing()
            .load()

        return DatabaseConfig(
            jdbcUrl = environment.requireValue(DATABASE_URL),
            username = environment.requireValue(DATABASE_USER),
            password = environment.requireValue(DATABASE_PASSWORD)
        )
    }

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

    private fun Dotenv.requireValue(name: String): String {
        return get(name)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw IllegalStateException(
                "Missing required database configuration: $name. " +
                    "Set it as an environment variable or in a local backend .env file."
            )
    }

    private data class DatabaseConfig(
        val jdbcUrl: String,
        val username: String,
        val password: String
    )
}
