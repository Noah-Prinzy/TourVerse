package com.tourverse.data.session

import com.tourverse.data.model.AuthResponse
import com.tourverse.data.model.User
import com.tourverse.data.remote.ApiClient
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionTokenStoreTest {
    @Test
    fun inMemoryStoreWritesAndClearsToken() {
        val store = InMemorySessionTokenStore()
        store.writeRefreshToken("refresh")
        assertEquals("refresh", store.readRefreshToken())
        store.clear()
        assertNull(store.readRefreshToken())
    }

    @Test
    fun authResponseParsesBackendContract() {
        val response = ApiClient.json.decodeFromString<AuthResponse>(
            """{"accessToken":"a","refreshToken":"r","tokenType":"Bearer","expiresInSeconds":3600,"user":{"id":"11111111-1111-4111-8111-111111111111","firstName":"Test","lastName":"User","email":"test@example.com","profileImageUrl":null,"bio":null,"role":"USER","createdAt":"2026-01-01T00:00:00Z"}}"""
        )
        assertEquals("r", response.refreshToken)
        assertEquals("USER", response.user.role)
    }
}
