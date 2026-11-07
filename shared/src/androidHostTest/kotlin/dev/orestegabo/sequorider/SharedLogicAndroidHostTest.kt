package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.core.auth.CurrentUser
import dev.orestegabo.sequorider.core.auth.hasRiderAccess
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {

    @Test
    fun riderWorkspaceRequiresAnActiveCourierRole() {
        val courier = CurrentUser("fixture", provider = "GOOGLE", status = "ACTIVE", roles = setOf("COURIER"))
        assertTrue(courier.hasRiderAccess())
        assertFalse(courier.copy(active = false).hasRiderAccess())
        assertFalse(courier.copy(roles = setOf("ADMIN", "SUPPORT_AGENT")).hasRiderAccess())
        assertFalse(courier.copy(roles = setOf("CUSTOMER")).hasRiderAccess())
    }


    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }
}