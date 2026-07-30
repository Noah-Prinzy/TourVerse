package com.tourverse.utils

import io.github.cdimascio.dotenv.Dotenv

// Provides shared app environment behavior without requiring an instance.
object AppEnvironment {
    private val dotenv: Dotenv by lazy { Dotenv.configure().ignoreIfMissing().load() }

    // Retrieves get needed by this flow.
    fun get(name: String): String? =
        System.getenv(name)?.trim()?.takeIf(String::isNotEmpty)
            ?: dotenv[name]?.trim()?.takeIf(String::isNotEmpty)

    // Validates require and rejects unsupported input.
    fun require(name: String): String = get(name)
        ?: throw IllegalStateException("Missing required configuration: $name")

    // Retrieves int needed by this flow.
    fun getInt(name: String, default: Int): Int = get(name)?.toIntOrNull() ?: default
    // Retrieves boolean needed by this flow.
    fun getBoolean(name: String, default: Boolean): Boolean =
        get(name)?.lowercase()?.let { it == "true" || it == "1" || it == "yes" } ?: default

    val environmentName: String get() = get("TOURVERSE_ENV") ?: "development"
    val isProduction: Boolean get() = environmentName.equals("production", ignoreCase = true)
}
