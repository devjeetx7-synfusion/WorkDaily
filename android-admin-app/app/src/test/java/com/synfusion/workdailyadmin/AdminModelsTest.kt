package com.synfusion.workdailyadmin

import com.synfusion.workdailyadmin.data.models.AdminRole
import com.synfusion.workdailyadmin.data.models.UserRestrictions
import com.synfusion.workdailyadmin.data.models.UserSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdminModelsTest {

    @Test
    fun testAdminRoleFromString() {
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("SUPER_ADMIN"))
        assertEquals(AdminRole.ADMIN, AdminRole.fromString("admin"))
        assertEquals(AdminRole.MODERATOR, AdminRole.fromString("MODERATOR"))
        assertEquals(AdminRole.SUPPORT, AdminRole.fromString("support"))
        assertEquals(AdminRole.ANALYST, AdminRole.fromString("unknown_role"))
        assertEquals(AdminRole.ANALYST, AdminRole.fromString(null))
    }

    @Test
    fun testAdminRoleHierarchy() {
        assertEquals(5, AdminRole.SUPER_ADMIN.rank)
        assertEquals(4, AdminRole.ADMIN.rank)
        assertEquals(3, AdminRole.MODERATOR.rank)
        assertEquals(2, AdminRole.SUPPORT.rank)
        assertEquals(1, AdminRole.ANALYST.rank)
    }

    @Test
    fun testUserSummaryDefaultValues() {
        val summary = UserSummary(
            uid = "test_uid_123",
            displayName = "Test Admin",
            email = "admin@workdaily.com"
        )

        assertEquals("test_uid_123", summary.uid)
        assertEquals("Test Admin", summary.displayName)
        assertEquals("admin@workdaily.com", summary.email)
        assertEquals("ACTIVE", summary.accountStatus)
        assertFalse(summary.isVerified)
        assertEquals(UserRestrictions(), summary.restrictions)
    }
}
