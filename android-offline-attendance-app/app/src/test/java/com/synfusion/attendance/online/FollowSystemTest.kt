package com.synfusion.attendance.online

import org.junit.Assert.*
import org.junit.Test

class FollowSystemTest {

    @Test
    fun testFollowRecordDataClassDefaults() {
        val now = System.currentTimeMillis()
        val record = FollowRecord(
            followerUid = "user_follower_123",
            targetUid = "user_target_456",
            createdAt = now
        )

        assertEquals("user_follower_123", record.followerUid)
        assertEquals("user_target_456", record.targetUid)
        assertEquals(now, record.createdAt)
    }

    @Test
    fun testPublicProfileFollowCounts() {
        val profile = PublicProfile(
            uid = "user_test_789",
            displayName = "Rajesh Sharma",
            username = "rajesh_sharma",
            followerCount = 10,
            followingCount = 5
        )

        assertEquals(10, profile.followerCount)
        assertEquals(5, profile.followingCount)

        // Test incrementing follower count after follow
        val updatedProfile = profile.copy(followerCount = profile.followerCount + 1)
        assertEquals(11, updatedProfile.followerCount)
        assertEquals(5, updatedProfile.followingCount)

        // Test decrementing follower count after unfollow
        val unfollowedProfile = updatedProfile.copy(followerCount = (updatedProfile.followerCount - 1).coerceAtLeast(0))
        assertEquals(10, unfollowedProfile.followerCount)
    }

    @Test
    fun testSearchQueryNormalization() {
        val rawInput1 = "  @rajesh_sharma  "
        val normalized1 = rawInput1.trim().removePrefix("@").trim()
        assertEquals("rajesh_sharma", normalized1)

        val rawInput2 = "@"
        val normalized2 = rawInput2.trim().removePrefix("@").trim()
        assertTrue(normalized2.isEmpty())
    }

    @Test
    fun testRecentQueriesDeduplication() {
        val recentQueries = listOf("rajesh", "pooja", "ramesh")
        val newQuery = "RAJESH"

        val clean = newQuery.trim().removePrefix("@").trim()
        val updated = (listOf(clean) + recentQueries.filterNot { it.equals(clean, ignoreCase = true) }).take(8)

        assertEquals(3, updated.size)
        assertEquals("RAJESH", updated[0])
        assertEquals("pooja", updated[1])
        assertEquals("ramesh", updated[2])
    }
}
