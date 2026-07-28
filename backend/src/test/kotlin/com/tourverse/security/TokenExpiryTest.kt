package com.tourverse.security

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNull

class TokenExpiryTest {
    @Test
    fun expiredAccessTokenIsRejected() {
        val issued = TokenService.createAccessToken(UUID.randomUUID(), "USER", lifetimeSeconds = -1)
        assertNull(TokenService.verifyAccessToken(issued.token))
    }
}
