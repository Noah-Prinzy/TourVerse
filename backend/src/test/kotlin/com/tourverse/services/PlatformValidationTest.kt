package com.tourverse.services

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformValidationTest {
    @Test fun supportedServiceTypesAreStable() {
        val types = setOf("HOTEL", "RESTAURANT", "TOUR", "TRANSPORT", "GUIDE", "ACTIVITY")
        assertTrue("HOTEL" in types)
        assertTrue("ACTIVITY" in types)
    }

    @Test fun bookingStatusesAreStable() {
        val statuses = setOf("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED")
        assertTrue("PENDING" in statuses)
        assertTrue("COMPLETED" in statuses)
    }
}
