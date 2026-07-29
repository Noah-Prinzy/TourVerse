package com.tourverse

import com.tourverse.services.CountryCodeService
import com.tourverse.utils.ValidationException
import kotlin.test.*

class CountryCodeServiceTest {
    @Test fun `codes normalize to uppercase`() = assertEquals("UG", CountryCodeService.normalizeCode(" ug "))
    @Test fun `recognized names and aliases resolve consistently`() {
        assertEquals("US", CountryCodeService.codeForName("United States of America"))
        assertEquals("US", CountryCodeService.codeForName("U.S.A."))
        assertEquals("United States", CountryCodeService.resolve("USA", null).first)
    }
    @Test fun `mismatched name and code are rejected`() {
        assertFailsWith<ValidationException> { CountryCodeService.resolve("France", "UG") }
    }
    @Test fun `unknown codes are rejected`() {
        assertFailsWith<ValidationException> { CountryCodeService.normalizeCode("ZZ") }
    }
}
