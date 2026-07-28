package com.tourverse.data.remote

import com.tourverse.BuildConfig
import com.tourverse.data.model.*
import com.tourverse.data.session.SessionTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

class AuthApi(
    private val tokenStore: SessionTokenStore,
    @PublishedApi internal val client: HttpClient = ApiClient.client,
    apiBaseUrl: String = BuildConfig.API_BASE_URL
) {
    @PublishedApi internal val baseUrl = apiBaseUrl.trim().also { require(it.isNotEmpty()) }
        .let { if (it.endsWith('/')) it else "$it/" }
    private val refreshMutex = Mutex()
    @PublishedApi @Volatile internal var accessToken: String? = null
    @Volatile var currentUser: User? = null
        private set

    suspend fun register(request: RegisterRequest) =
        publicPost<AuthResponse, RegisterRequest>("api/auth/register", request).also(::establish)
    suspend fun login(request: LoginRequest) =
        publicPost<AuthResponse, LoginRequest>("api/auth/login", request).also(::establish)

    suspend fun restore(): Boolean {
        val token = tokenStore.readRefreshToken() ?: return false
        return runCatching { refreshWith(token); true }.getOrElse { clear(); false }
    }

    suspend fun logout(all: Boolean = false) {
        val refresh = tokenStore.readRefreshToken()
        try {
            if (all) authenticated<Unit>("api/auth/logout-all", HttpMethod.Post)
            else if (refresh != null) {
                publicPost<ApiMessage, LogoutRequest>("api/auth/logout", LogoutRequest(refresh))
            }
        } finally { clear() }
    }

    suspend fun profile(): UserProfile = authenticated("api/users/me/profile", HttpMethod.Get)
    suspend fun updateProfile(request: UpdateUserProfileRequest): UserProfile =
        authenticated("api/users/me/profile", HttpMethod.Put, request)
    suspend fun updateProfileImage(request: UpdateProfileImageRequest): UserProfile =
        authenticated("api/users/me/profile/image", HttpMethod.Put, request)
    suspend fun deleteAccount(request: DeleteAccountRequest): ApiMessage =
        authenticated("api/users/me", HttpMethod.Delete, request)

    suspend inline fun <reified T> get(path: String): T = authenticated(path, HttpMethod.Get)
    suspend inline fun <reified T, reified B> post(path: String, body: B): T = authenticated(path, HttpMethod.Post, body)
    suspend inline fun <reified T> post(path: String): T = authenticated(path, HttpMethod.Post)
    suspend inline fun <reified T, reified B> put(path: String, body: B): T = authenticated(path, HttpMethod.Put, body)
    suspend inline fun <reified T> delete(path: String): T = authenticated(path, HttpMethod.Delete)

    suspend inline fun <reified T, reified B> publicPost(path: String, body: B): T {
        val response = safeRequest { client.post("$baseUrl$path") { contentType(ContentType.Application.Json); setBody(body) } }
        return decode(response)
    }

    suspend inline fun <reified T> authenticated(path: String, method: HttpMethod, body: Any? = null): T {
        val usedToken = accessToken
        var response = safeRequest { request(path, method, usedToken, body) }
        if (response.status == HttpStatusCode.Unauthorized && refreshIfNeeded(usedToken)) {
            response = safeRequest { request(path, method, accessToken, body) }
        }
        return decode(response)
    }

    @PublishedApi internal suspend fun request(path: String, method: HttpMethod, token: String?, body: Any?): HttpResponse =
        client.request("$baseUrl$path") {
            this.method = method
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
        }

    @PublishedApi internal suspend fun refreshIfNeeded(failedToken: String?): Boolean = refreshMutex.withLock {
        if (accessToken != null && accessToken != failedToken) return@withLock true
        val refresh = tokenStore.readRefreshToken() ?: return@withLock false
        runCatching { refreshWith(refresh); true }.getOrElse { clear(); false }
    }

    private suspend fun refreshWith(token: String) {
        establish(publicPost<AuthResponse, RefreshTokenRequest>("api/auth/refresh", RefreshTokenRequest(token)))
    }

    private fun establish(response: AuthResponse) {
        accessToken = response.accessToken
        currentUser = response.user
        tokenStore.writeRefreshToken(response.refreshToken)
    }

    fun clear() {
        accessToken = null
        currentUser = null
        tokenStore.clear()
    }

    @PublishedApi internal suspend fun safeRequest(block: suspend () -> HttpResponse): HttpResponse = try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        throw AuthApiException("Unable to connect to TourVerse. Check your connection and try again.", exception)
    }

    @PublishedApi internal suspend inline fun <reified T> decode(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            if (T::class == Unit::class) return Unit as T
            return try { response.body() } catch (exception: Exception) {
                throw AuthApiException("TourVerse returned an invalid response.", exception)
            }
        }
        val fallback = "Request failed (HTTP ${response.status.value})."
        val message = try {
            ApiClient.json.decodeFromString<ApiMessage>(response.bodyAsText()).message.ifBlank { fallback }
        } catch (_: SerializationException) { fallback }
        catch (_: IllegalArgumentException) { fallback }
        throw AuthApiException(message)
    }
}

class AuthApiException(override val message: String, cause: Throwable? = null) : Exception(message, cause)
