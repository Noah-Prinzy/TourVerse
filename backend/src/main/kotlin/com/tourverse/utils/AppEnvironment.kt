package com.tourverse.utils

import io.github.cdimascio.dotenv.Dotenv

object AppEnvironment {
    private val dotenv: Dotenv by lazy { Dotenv.configure().ignoreIfMissing().load() }

    fun get(name: String): String? =
        System.getenv(name)?.trim()?.takeIf(String::isNotEmpty)
            ?: dotenv[name]?.trim()?.takeIf(String::isNotEmpty)

    fun require(name: String): String = get(name)
        ?: throw IllegalStateException("Missing required configuration: $name")

    fun getInt(name: String, default: Int): Int = get(name)?.toIntOrNull() ?: default
    fun getBoolean(name: String, default: Boolean): Boolean =
        get(name)?.lowercase()?.let { it == "true" || it == "1" || it == "yes" } ?: default

    val environmentName: String get() = get("TOURVERSE_ENV") ?: "development"
    val isProduction: Boolean get() = environmentName.equals("production", ignoreCase = true)
}
