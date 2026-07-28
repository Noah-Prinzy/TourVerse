package com.tourverse.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun `hash verifies correct password`() {
        val hash = PasswordHasher.hash("SecurePass1")
        assertTrue(PasswordHasher.verify("SecurePass1", hash))
    }

    @Test
    fun `hash rejects incorrect password`() {
        val hash = PasswordHasher.hash("SecurePass1")
        assertFalse(PasswordHasher.verify("WrongPass1", hash))
    }

    @Test
    fun `same password receives different salts`() {
        assertNotEquals(PasswordHasher.hash("SecurePass1"), PasswordHasher.hash("SecurePass1"))
    }
}
