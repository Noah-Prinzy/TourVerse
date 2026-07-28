package com.tourverse.security

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TokenServiceTest {
    @Test
    fun `valid access token can be verified`() {
        val userId = UUID.randomUUID()
        val issued = TokenService.createAccessToken(userId, "USER")
        val claims = assertNotNull(TokenService.verifyAccessToken(issued.token))
        assertEquals(userId.toString(), claims.userId)
        assertEquals("USER", claims.role)
    }

    @Test
    fun `modified access token is rejected`() {
        val token = TokenService.createAccessToken(UUID.randomUUID(), "USER").token
        assertNull(TokenService.verifyAccessToken(token + "changed"))
    }
}
