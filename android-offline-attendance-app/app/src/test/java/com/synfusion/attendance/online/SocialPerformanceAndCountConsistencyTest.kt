package com.synfusion.attendance.online

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SocialPerformanceAndCountConsistencyTest {

    private class FakePublicProfileRepo : PublicProfileRepository {
        val profiles = mutableMapOf<String, PublicProfile>()
        val getProfileCalls = AtomicInteger(0)
        val searchCalls = AtomicInteger(0)

        override suspend fun getPublicProfile(firebaseUid: String): ProfileOperationResult<PublicProfile?> {
            getProfileCalls.incrementAndGet()
            return ProfileOperationResult.Success(profiles[firebaseUid])
        }

        override suspend fun checkUsernameAvailability(username: String): UsernameAvailabilityResult {
            return UsernameAvailabilityResult.Available
        }

        override suspend fun ensureOrCreatePublicProfile(
            firebaseUid: String,
            displayName: String?,
            photoUrl: String?
        ): ProfileOperationResult<PublicProfile> {
            val created = PublicProfile(uid = firebaseUid, displayName = displayName.orEmpty(), photoUrl = photoUrl)
            profiles[firebaseUid] = created
            return ProfileOperationResult.Success(created)
        }

        override suspend fun updatePublicProfile(
            firebaseUid: String,
            newUsername: String,
            displayName: String,
            bio: String,
            profession: String,
            photoUrl: String?,
            isPublic: Boolean,
            gender: String,
            dateOfBirth: String?,
            countryCode: String
        ): ProfileOperationResult<PublicProfile> {
            val existing = profiles[firebaseUid] ?: PublicProfile(uid = firebaseUid)
            val updated = existing.copy(
                username = newUsername,
                displayName = displayName,
                bio = bio,
                profession = profession,
                photoUrl = photoUrl,
                isPublic = isPublic,
                // Preserve the optional attributes a caller may not manage, so the Google
                // sign-up path (which relies on the interface defaults "", null, "") cannot
                // erase gender/dateOfBirth/countryCode already saved from Settings.
                gender = gender.ifBlank { existing.gender },
                dateOfBirth = dateOfBirth?.takeIf { it.isNotBlank() } ?: existing.dateOfBirth,
                countryCode = countryCode.ifBlank { existing.countryCode }
            )
            profiles[firebaseUid] = updated
            return ProfileOperationResult.Success(updated)
        }

        override suspend fun searchPublicProfiles(
            query: String,
            currentUserId: String?,
            limit: Int
        ): ProfileOperationResult<List<PublicProfile>> {
            searchCalls.incrementAndGet()
            val normalized = query.trim().removePrefix("@").lowercase()
            val matches = profiles.values
                .filter { it.uid != currentUserId }
                .filter {
                    it.username.lowercase().startsWith(normalized) ||
                        it.displayName.lowercase().startsWith(normalized)
                }
                .take(limit)
            return ProfileOperationResult.Success(matches)
        }
    }

    private class FakeFollowRepo(
        private val profileStore: Map<String, PublicProfile>
    ) : FollowRepository {
        val edges = linkedSetOf<Pair<String, String>>() // (followerUid, targetUid)
        val getCountsCalls = AtomicInteger(0)
        val followCalls = AtomicInteger(0)
        val unfollowCalls = AtomicInteger(0)

        var countsBarrier: CompletableDeferred<Unit>? = null
        var followBarrier: CompletableDeferred<Unit>? = null
        var nextFollowError: String? = null
        var nextCountsError: String? = null

        override suspend fun followUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean> {
            followCalls.incrementAndGet()
            followBarrier?.await()
            nextFollowError?.let { msg ->
                nextFollowError = null
                return ProfileOperationResult.Error(msg)
            }
            if (followerUid == targetUid) {
                return ProfileOperationResult.Error("Cannot follow yourself")
            }
            val added = edges.add(followerUid to targetUid)
            return ProfileOperationResult.Success(added)
        }

        override suspend fun unfollowUser(followerUid: String, targetUid: String): ProfileOperationResult<Boolean> {
            unfollowCalls.incrementAndGet()
            followBarrier?.await()
            nextFollowError?.let { msg ->
                nextFollowError = null
                return ProfileOperationResult.Error(msg)
            }
            val removed = edges.remove(followerUid to targetUid)
            return ProfileOperationResult.Success(removed)
        }

        override suspend fun isFollowing(followerUid: String, targetUid: String): Boolean {
            return edges.contains(followerUid to targetUid)
        }

        override suspend fun isMutual(uidA: String, uidB: String): Boolean {
            return isFollowing(uidA, uidB) && isFollowing(uidB, uidA)
        }

        override suspend fun getFollowers(targetUid: String, limit: Int): ProfileOperationResult<List<PublicProfile>> {
            val followerIds = edges.filter { it.second == targetUid }.map { it.first }.take(limit)
            val profiles = followerIds.map { uid ->
                profileStore[uid] ?: PublicProfile(uid = uid, displayName = "WorkDaily Member")
            }
            return ProfileOperationResult.Success(profiles)
        }

        override suspend fun getFollowing(followerUid: String, limit: Int): ProfileOperationResult<List<PublicProfile>> {
            val followingIds = edges.filter { it.first == followerUid }.map { it.second }.take(limit)
            val profiles = followingIds.map { uid ->
                profileStore[uid] ?: PublicProfile(uid = uid, displayName = "WorkDaily Member")
            }
            return ProfileOperationResult.Success(profiles)
        }

        override suspend fun getFollowCounts(uid: String): ProfileOperationResult<FollowCounts> {
            getCountsCalls.incrementAndGet()
            countsBarrier?.await()
            nextCountsError?.let { msg ->
                nextCountsError = null
                return ProfileOperationResult.Error(msg)
            }
            val followers = edges.count { it.second == uid }
            val following = edges.count { it.first == uid }
            return ProfileOperationResult.Success(FollowCounts(followerCount = followers, followingCount = following))
        }
    }

    private class FakeBlockRepo : BlockRepository {
        val blocked = mutableSetOf<Pair<String, String>>()

        override suspend fun blockUser(blockerUid: String, blockedUid: String): ProfileOperationResult<Boolean> {
            blocked.add(blockerUid to blockedUid)
            return ProfileOperationResult.Success(true)
        }

        override suspend fun unblockUser(blockerUid: String, blockedUid: String): ProfileOperationResult<Boolean> {
            blocked.remove(blockerUid to blockedUid)
            return ProfileOperationResult.Success(true)
        }

        override suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean {
            return blocked.contains(blockerUid to blockedUid)
        }

        override suspend fun isEitherBlocked(uidA: String, uidB: String): Boolean {
            return isBlocked(uidA, uidB) || isBlocked(uidB, uidA)
        }

        override suspend fun getBlockedUserIds(blockerUid: String): ProfileOperationResult<List<String>> {
            return ProfileOperationResult.Success(blocked.filter { it.first == blockerUid }.map { it.second })
        }

        override suspend fun removeFollower(targetUid: String, followerUid: String): ProfileOperationResult<Boolean> {
            return ProfileOperationResult.Success(true)
        }

        override suspend fun getBlockedProfiles(blockerUid: String): ProfileOperationResult<List<PublicProfile>> {
            return ProfileOperationResult.Success(emptyList())
        }
    }

    private fun sampleProfile(uid: String, username: String, name: String): PublicProfile {
        return PublicProfile(
            uid = uid,
            username = username,
            usernameLower = username.lowercase(),
            displayName = name,
            displayNameLower = name.lowercase()
        )
    }

    @Test
    fun `follow and unfollow update both personal followingCount and target followerCount consistently`() = runTest {
        var now = 1_000L
        val profileRepo = FakePublicProfileRepo().apply {
            profiles["uid_a"] = sampleProfile("uid_a", "arjun", "Arjun Kumar")
            profiles["uid_b"] = sampleProfile("uid_b", "priya", "Priya Singh")
        }
        val followRepo = FakeFollowRepo(profileRepo.profiles)
        val blockRepo = FakeBlockRepo()
        val manager = SocialSessionManager(
            publicProfileRepository = profileRepo,
            followRepository = followRepo,
            blockRepository = blockRepo,
            clockMs = { now }
        )

        val initialOwnCounts = manager.getFollowCounts("uid_a", "uid_a")
        val initialTargetCounts = manager.getFollowCounts("uid_a", "uid_b")
        assertEquals(FollowCounts(0, 0), (initialOwnCounts as ProfileOperationResult.Success).data)
        assertEquals(FollowCounts(0, 0), (initialTargetCounts as ProfileOperationResult.Success).data)

        // User A follows User B
        val followRes = manager.setFollowState(
            actorUid = "uid_a",
            targetUid = "uid_b",
            wantFollow = true,
            targetProfileHint = profileRepo.profiles.getValue("uid_b")
        )
        assertTrue(followRes is ProfileOperationResult.Success)

        val snapshotAfterFollow = manager.currentSnapshotFor("uid_a")
        assertNotNull(snapshotAfterFollow)
        assertEquals(FollowCounts(followerCount = 0, followingCount = 1), snapshotAfterFollow?.followCounts?.get("uid_a")?.value)
        assertEquals(FollowCounts(followerCount = 1, followingCount = 0), snapshotAfterFollow?.followCounts?.get("uid_b")?.value)
        assertTrue(snapshotAfterFollow?.myFollowedUids?.value?.contains("uid_b") == true)

        // User A unfollows User B
        val unfollowRes = manager.setFollowState(
            actorUid = "uid_a",
            targetUid = "uid_b",
            wantFollow = false,
            targetProfileHint = profileRepo.profiles.getValue("uid_b")
        )
        assertTrue(unfollowRes is ProfileOperationResult.Success)

        val snapshotAfterUnfollow = manager.currentSnapshotFor("uid_a")
        assertNotNull(snapshotAfterUnfollow)
        assertEquals(FollowCounts(followerCount = 0, followingCount = 0), snapshotAfterUnfollow?.followCounts?.get("uid_a")?.value)
        assertEquals(FollowCounts(followerCount = 0, followingCount = 0), snapshotAfterUnfollow?.followCounts?.get("uid_b")?.value)
        assertFalse(snapshotAfterUnfollow?.myFollowedUids?.value?.contains("uid_b") == true)
    }

    @Test
    fun `failed follow rolls back optimistic counts and following set cleanly`() = runTest {
        val profileRepo = FakePublicProfileRepo().apply {
            profiles["uid_a"] = sampleProfile("uid_a", "arjun", "Arjun")
            profiles["uid_b"] = sampleProfile("uid_b", "priya", "Priya")
        }
        val followRepo = FakeFollowRepo(profileRepo.profiles)
        val manager = SocialSessionManager(
            publicProfileRepository = profileRepo,
            followRepository = followRepo,
            blockRepository = FakeBlockRepo(),
            clockMs = { 5_000L }
        )

        manager.getFollowCounts("uid_a", "uid_a")
        manager.getFollowCounts("uid_a", "uid_b")
        manager.getFollowingList("uid_a", "uid_a")

        followRepo.nextFollowError = "Permission denied"
        val res = manager.setFollowState(
            actorUid = "uid_a",
            targetUid = "uid_b",
            wantFollow = true,
            targetProfileHint = profileRepo.profiles.getValue("uid_b")
        )

        assertTrue(res is ProfileOperationResult.Error)
        val snap = manager.currentSnapshotFor("uid_a")
        assertNotNull(snap)
        assertEquals(FollowCounts(0, 0), snap?.followCounts?.get("uid_a")?.value)
        assertEquals(FollowCounts(0, 0), snap?.followCounts?.get("uid_b")?.value)
        assertFalse(snap?.myFollowedUids?.value?.contains("uid_b") == true)
        assertTrue(snap?.followingLists?.get("uid_a")?.value?.users.orEmpty().isEmpty())
        assertTrue(snap?.activeMutations.orEmpty().isEmpty())
    }

    @Test
    fun `duplicate concurrent follow taps ignore second in-flight tap without double incrementing counts`() = runTest {
        val profileRepo = FakePublicProfileRepo().apply {
            profiles["uid_a"] = sampleProfile("uid_a", "arjun", "Arjun")
            profiles["uid_b"] = sampleProfile("uid_b", "priya", "Priya")
        }
        val followRepo = FakeFollowRepo(profileRepo.profiles)
        val barrier = CompletableDeferred<Unit>()
        followRepo.followBarrier = barrier

        val manager = SocialSessionManager(
            publicProfileRepository = profileRepo,
            followRepository = followRepo,
            blockRepository = FakeBlockRepo(),
            clockMs = { 10_000L }
        )
        manager.getFollowCounts("uid_a", "uid_a")
        manager.getFollowCounts("uid_a", "uid_b")

        val firstTap = async {
            manager.setFollowState(
                actorUid = "uid_a",
                targetUid = "uid_b",
                wantFollow = true,
                targetProfileHint = profileRepo.profiles.getValue("uid_b")
            )
        }
        testScheduler.runCurrent() // Run firstTap until it suspends at followBarrier

        val secondTap = async {
            manager.setFollowState(
                actorUid = "uid_a",
                targetUid = "uid_b",
                wantFollow = true,
                targetProfileHint = profileRepo.profiles.getValue("uid_b")
            )
        }
        testScheduler.runCurrent()

        barrier.complete(Unit)
        val results = listOf(firstTap.await(), secondTap.await())
        assertEquals(1, results.count { it is ProfileOperationResult.Success })
        assertEquals(1, results.count { it == null })

        assertEquals(1, followRepo.followCalls.get())
        val snap = manager.currentSnapshotFor("uid_a")
        assertNotNull(snap)
        assertEquals(1, snap?.followCounts?.get("uid_a")?.value?.followingCount)
        assertEquals(1, snap?.followCounts?.get("uid_b")?.value?.followerCount)
    }

    @Test
    fun `concurrent getFollowCounts requests coalesce and warm cache avoids duplicate network calls`() = runTest {
        var now = 20_000L
        val profileRepo = FakePublicProfileRepo()
        val followRepo = FakeFollowRepo(profileRepo.profiles).apply {
            edges.add("uid_x" to "uid_a")
            edges.add("uid_y" to "uid_a")
            edges.add("uid_a" to "uid_z")
        }
        val barrier = CompletableDeferred<Unit>()
        followRepo.countsBarrier = barrier

        val manager = SocialSessionManager(
            publicProfileRepository = profileRepo,
            followRepository = followRepo,
            blockRepository = FakeBlockRepo(),
            clockMs = { now }
        )

        val call1 = async { manager.getFollowCounts("uid_a", "uid_a") }
        val call2 = async { manager.getFollowCounts("uid_a", "uid_a") }
        barrier.complete(Unit)

        val res1 = call1.await() as ProfileOperationResult.Success
        val res2 = call2.await() as ProfileOperationResult.Success
        assertEquals(FollowCounts(followerCount = 2, followingCount = 1), res1.data)
        assertEquals(FollowCounts(followerCount = 2, followingCount = 1), res2.data)
        assertEquals(1, followRepo.getCountsCalls.get())

        // Subsequent call within TTL returns cached counts immediately without hitting Firestore
        followRepo.countsBarrier = null
        now += 5_000L
        val cachedHit = manager.getFollowCounts("uid_a", "uid_a") as ProfileOperationResult.Success
        assertEquals(FollowCounts(followerCount = 2, followingCount = 1), cachedHit.data)
        assertEquals(1, followRepo.getCountsCalls.get())
    }

    @Test
    fun `background count refresh failure preserves valid cached non-zero counts`() = runTest {
        var now = 1_000L
        val profileRepo = FakePublicProfileRepo()
        val followRepo = FakeFollowRepo(profileRepo.profiles).apply {
            edges.add("uid_b" to "uid_a")
            edges.add("uid_a" to "uid_c")
        }
        val manager = SocialSessionManager(
            publicProfileRepository = profileRepo,
            followRepository = followRepo,
            blockRepository = FakeBlockRepo(),
            clockMs = { now }
        )

        val initial = manager.getFollowCounts("uid_a", "uid_a") as ProfileOperationResult.Success
        assertEquals(FollowCounts(1, 1), initial.data)

        // Force refresh fails transiently -> cached counts must stay (1, 1), never reset to (0, 0)
        followRepo.nextCountsError = "Network unavailable"
        val failedRefresh = manager.getFollowCounts("uid_a", "uid_a", forceRefresh = true)
        assertTrue(failedRefresh is ProfileOperationResult.Error)
        assertEquals(
            FollowCounts(1, 1),
            manager.currentSnapshotFor("uid_a")?.followCounts?.get("uid_a")?.value
        )
    }

    @Test
    fun `logout and account switch strictly isolate UID caches including chat messages and searches`() = runTest {
        var externalClearedCount = 0
        val profileRepo = FakePublicProfileRepo().apply {
            profiles["uid_b"] = sampleProfile("uid_b", "priya", "Priya")
        }
        val followRepo = FakeFollowRepo(profileRepo.profiles).apply {
            edges.add("uid_a" to "uid_b")
        }
        val manager = SocialSessionManager(
            publicProfileRepository = profileRepo,
            followRepository = followRepo,
            blockRepository = FakeBlockRepo(),
            clockMs = { 1_000L },
            onClearExternalCaches = { externalClearedCount += 1 }
        )
        manager.onAuthenticatedUserChanged("uid_a")
        externalClearedCount = 0

        manager.getFollowCounts("uid_a", "uid_a")
        manager.getFollowingList("uid_a", "uid_a")
        manager.searchProfiles("uid_a", "@pri")
        manager.cacheConversationMessages(
            ownerUid = "uid_a",
            conversationId = "uid_a_uid_b",
            messages = listOf(
                FirestoreChatMessage(
                    id = "m1",
                    senderUid = "uid_a",
                    text = "Private message for A",
                    timestamp = 1_000L,
                    status = MessageStatus.SENT
                )
            )
        )

        assertNotNull(manager.currentSnapshotFor("uid_a")?.chatMessages?.get("uid_a_uid_b"))
        assertNull(manager.currentSnapshotFor("uid_c")?.chatMessages?.get("uid_a_uid_b"))

        // Switch to User C -> User A's cached data is completely purged
        manager.onAuthenticatedUserChanged("uid_c")
        assertNull(manager.currentSnapshotFor("uid_a"))
        assertNull(manager.currentSnapshotFor("uid_a")?.chatMessages?.get("uid_a_uid_b"))
        assertTrue(manager.currentSnapshotFor("uid_c")?.followCounts.orEmpty().isEmpty())
        assertEquals(1, externalClearedCount)

        // Sign out -> all session data cleared
        manager.clear()
        assertNull(manager.snapshot.value.ownerUid)
        assertEquals(2, externalClearedCount)
    }
}
