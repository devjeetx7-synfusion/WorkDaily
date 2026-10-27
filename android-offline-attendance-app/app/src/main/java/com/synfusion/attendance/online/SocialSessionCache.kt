package com.synfusion.attendance.online

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimedEntry<out T>(
    val value: T,
    val updatedAtMs: Long,
    val isAuthoritative: Boolean = true
) {
    fun isFresh(nowMs: Long, ttlMs: Long): Boolean = (nowMs - updatedAtMs) in 0..ttlMs
}

data class PagedUsersEntry(
    val users: List<PublicProfile>,
    val requestedLimit: Int,
    val hasMore: Boolean = users.size >= requestedLimit
)

data class ChatPeerRelationship(
    val friendUid: String,
    val isMutual: Boolean,
    val isBlockedByMe: Boolean,
    val isBlockedByPeer: Boolean
) {
    val isBlockedEitherWay: Boolean get() = isBlockedByMe || isBlockedByPeer
    val canMessage: Boolean get() = isMutual && !isBlockedEitherWay
}

data class SocialUserSnapshot(
    val ownerUid: String? = null,
    val profiles: Map<String, TimedEntry<PublicProfile>> = emptyMap(),
    val followCounts: Map<String, TimedEntry<FollowCounts>> = emptyMap(),
    val myFollowedUids: TimedEntry<Set<String>>? = null,
    val myFollowerUids: TimedEntry<Set<String>>? = null,
    val myBlockedUids: TimedEntry<Set<String>>? = null,
    val followersLists: Map<String, TimedEntry<PagedUsersEntry>> = emptyMap(),
    val followingLists: Map<String, TimedEntry<PagedUsersEntry>> = emptyMap(),
    val chatRelationships: Map<String, TimedEntry<ChatPeerRelationship>> = emptyMap(),
    val chatMessages: Map<String, List<FirestoreChatMessage>> = emptyMap(),
    val searchResults: Map<String, TimedEntry<List<PublicProfile>>> = emptyMap(),
    val activeMutations: Set<String> = emptySet()
)

/**
 * UID-isolated social state coordinator, in-memory cache, and in-flight request deduplicator.
 *
 * SECURITY & ISOLATION GUARANTEES:
 * - Every cached entry is bound strictly to [SocialUserSnapshot.ownerUid].
 * - Switching accounts or signing out atomically clears all user-specific state and cancels/invalidates in-flight entries.
 * - Coalesces concurrent requests for identical resources to prevent duplicate Firestore queries.
 * - Deduplicates Follow/Unfollow/Block mutations and rolls back optimistic updates cleanly on error.
 */
class SocialSessionManager(
    private val publicProfileRepository: PublicProfileRepository,
    private val followRepository: FollowRepository,
    private val blockRepository: BlockRepository,
    private val onClearExternalCaches: () -> Unit = {},
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val PROFILE_TTL_MS = 45_000L
        const val COUNTS_TTL_MS = 25_000L
        const val LIST_TTL_MS = 25_000L
        const val SEARCH_TTL_MS = 30_000L
        const val CHAT_REL_TTL_MS = 20_000L
    }

    private val lock = Any()
    private var sessionEpoch: Long = 0L
    private val inFlightMap = mutableMapOf<String, Deferred<Any?>>()

    private val _snapshot = MutableStateFlow(SocialUserSnapshot())
    val snapshot: StateFlow<SocialUserSnapshot> = _snapshot.asStateFlow()

    /**
     * Ensures the cache belongs to [authenticatedUid]. If the UID changed or became null/blank,
     * all cached state from the previous account is immediately wiped.
     */
    fun onAuthenticatedUserChanged(authenticatedUid: String?) {
        val cleanUid = authenticatedUid?.trim()?.takeIf { it.isNotBlank() }
        var shouldClearExternal = false
        synchronized(lock) {
            if (_snapshot.value.ownerUid != cleanUid) {
                sessionEpoch++
                val toCancel = inFlightMap.values.toList()
                inFlightMap.clear()
                toCancel.forEach { it.cancel() }
                _snapshot.value = SocialUserSnapshot(ownerUid = cleanUid)
                shouldClearExternal = true
            }
        }
        if (shouldClearExternal) {
            onClearExternalCaches()
        }
    }

    fun clear() {
        onAuthenticatedUserChanged(null)
    }

    fun currentSnapshotFor(uid: String?): SocialUserSnapshot? {
        val clean = uid?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val snap = _snapshot.value
        return if (snap.ownerUid == clean) snap else null
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> coalesce(
        ownerUid: String,
        key: String,
        block: suspend (epochAtStart: Long) -> T
    ): T {
        onAuthenticatedUserChanged(ownerUid)
        val fullKey = "$ownerUid::$key"
        val deferredToAwait: Deferred<T>?
        val createdDeferred: CompletableDeferred<Any?>?
        val capturedEpoch: Long

        synchronized(lock) {
            capturedEpoch = sessionEpoch
            val existing = inFlightMap[fullKey]
            if (existing != null && existing.isActive) {
                deferredToAwait = existing as Deferred<T>
                createdDeferred = null
            } else {
                val created = CompletableDeferred<Any?>()
                inFlightMap[fullKey] = created
                deferredToAwait = null
                createdDeferred = created
            }
        }

        val existingDeferred = deferredToAwait
        if (existingDeferred != null) {
            return existingDeferred.await()
        }

        val myDeferred = createdDeferred ?: CompletableDeferred()
        try {
            val result = block(capturedEpoch)
            myDeferred.complete(result)
            return result
        } catch (ce: CancellationException) {
            myDeferred.cancel(ce)
            throw ce
        } catch (t: Throwable) {
            myDeferred.completeExceptionally(t)
            throw t
        } finally {
            synchronized(lock) {
                if (inFlightMap[fullKey] === myDeferred) {
                    inFlightMap.remove(fullKey)
                }
            }
        }
    }

    private fun updateForOwner(
        ownerUid: String,
        expectedEpoch: Long? = null,
        transform: (SocialUserSnapshot) -> SocialUserSnapshot
    ) {
        synchronized(lock) {
            if (expectedEpoch != null && sessionEpoch != expectedEpoch) return
            val current = _snapshot.value
            if (current.ownerUid != ownerUid) return
            _snapshot.value = transform(current)
        }
    }

    suspend fun getPublicProfile(
        ownerUid: String,
        targetUid: String,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<PublicProfile?> {
        if (ownerUid.isBlank() || targetUid.isBlank()) {
            return ProfileOperationResult.Error("Invalid UID")
        }
        onAuthenticatedUserChanged(ownerUid)
        val now = clockMs()
        val cached = _snapshot.value.profiles[targetUid]
        if (!forceRefresh && cached != null && cached.isFresh(now, PROFILE_TTL_MS)) {
            return ProfileOperationResult.Success(cached.value)
        }

        return coalesce(ownerUid, "profile:$targetUid") { epoch ->
            when (val res = publicProfileRepository.getPublicProfile(targetUid)) {
                is ProfileOperationResult.Success -> {
                    val loaded = res.data
                    if (loaded != null) {
                        val stamp = clockMs()
                        updateForOwner(ownerUid, epoch) { snap ->
                            val existingCounts = snap.followCounts[targetUid]
                            val mergedProfile = if (existingCounts != null && existingCounts.isAuthoritative) {
                                loaded.copy(
                                    followerCount = existingCounts.value.followerCount,
                                    followingCount = existingCounts.value.followingCount
                                )
                            } else {
                                loaded
                            }
                            val nextCounts = if (existingCounts == null) {
                                snap.followCounts + (
                                    targetUid to TimedEntry(
                                        value = FollowCounts(
                                            followerCount = loaded.followerCount.coerceAtLeast(0),
                                            followingCount = loaded.followingCount.coerceAtLeast(0)
                                        ),
                                        updatedAtMs = stamp,
                                        isAuthoritative = false
                                    )
                                )
                            } else {
                                snap.followCounts
                            }
                            snap.copy(
                                profiles = snap.profiles + (targetUid to TimedEntry(mergedProfile, stamp)),
                                followCounts = nextCounts
                            )
                        }
                    }
                    res
                }
                is ProfileOperationResult.Error -> res
            }
        }
    }

    /**
     * Immediately seeds an updated [PublicProfile] into [ownerUid]'s session cache (e.g. after editing own profile)
     * while preserving any authoritative follow counts already cached for that UID.
     */
    fun seedProfile(ownerUid: String, profile: PublicProfile) {
        if (ownerUid.isBlank() || profile.uid.isBlank()) return
        val stamp = clockMs()
        updateForOwner(ownerUid) { snap ->
            val existingCounts = snap.followCounts[profile.uid]
            val mergedProfile = if (existingCounts != null) {
                profile.copy(
                    followerCount = existingCounts.value.followerCount,
                    followingCount = existingCounts.value.followingCount
                )
            } else {
                profile
            }
            snap.copy(
                profiles = snap.profiles + (profile.uid to TimedEntry(mergedProfile, stamp, isAuthoritative = true))
            )
        }
    }

    /**
     * Loads authoritative relationship counts for [targetUid] under [ownerUid]'s session.
     * Never fabricates zero on network failure; falls back to `public_profiles/{targetUid}` only if
     * that document actually exists, otherwise propagates [ProfileOperationResult.Error].
     */
    suspend fun getFollowCounts(
        ownerUid: String,
        targetUid: String,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<FollowCounts> {
        if (ownerUid.isBlank() || targetUid.isBlank()) {
            return ProfileOperationResult.Error("UID must not be blank.")
        }
        onAuthenticatedUserChanged(ownerUid)
        val now = clockMs()
        val cached = _snapshot.value.followCounts[targetUid]
        if (!forceRefresh && cached != null && cached.isAuthoritative && cached.isFresh(now, COUNTS_TTL_MS)) {
            return ProfileOperationResult.Success(cached.value)
        }

        return coalesce(ownerUid, "counts:$targetUid") { epoch ->
            when (val countsRes = followRepository.getFollowCounts(targetUid)) {
                is ProfileOperationResult.Success -> {
                    val exact = FollowCounts(
                        followerCount = countsRes.data.followerCount.coerceAtLeast(0),
                        followingCount = countsRes.data.followingCount.coerceAtLeast(0)
                    )
                    val stamp = clockMs()
                    updateForOwner(ownerUid, epoch) { snap ->
                        val updatedProfiles = snap.profiles[targetUid]?.let { existingProf ->
                            snap.profiles + (
                                targetUid to existingProf.copy(
                                    value = existingProf.value.copy(
                                        followerCount = exact.followerCount,
                                        followingCount = exact.followingCount
                                    ),
                                    updatedAtMs = stamp
                                )
                            )
                        } ?: snap.profiles
                        snap.copy(
                            followCounts = snap.followCounts + (
                                targetUid to TimedEntry(exact, stamp, isAuthoritative = true)
                            ),
                            profiles = updatedProfiles
                        )
                    }
                    ProfileOperationResult.Success(exact)
                }
                is ProfileOperationResult.Error -> {
                    // Fallback to stored profile document ONLY if it exists on Firestore/cache
                    val profileFallback = publicProfileRepository.getPublicProfile(targetUid)
                    if (profileFallback is ProfileOperationResult.Success && profileFallback.data != null) {
                        val prof = profileFallback.data
                        val fallbackCounts = FollowCounts(
                            followerCount = prof.followerCount.coerceAtLeast(0),
                            followingCount = prof.followingCount.coerceAtLeast(0)
                        )
                        val stamp = clockMs()
                        updateForOwner(ownerUid, epoch) { snap ->
                            val existing = snap.followCounts[targetUid]
                            val nextCounts = if (existing != null && existing.isAuthoritative) {
                                existing.value
                            } else {
                                fallbackCounts
                            }
                            snap.copy(
                                profiles = snap.profiles + (targetUid to TimedEntry(prof, stamp)),
                                followCounts = snap.followCounts + (
                                    targetUid to TimedEntry(
                                        nextCounts,
                                        stamp,
                                        isAuthoritative = existing?.isAuthoritative == true
                                    )
                                )
                            )
                        }
                        val resolved = _snapshot.value.followCounts[targetUid]?.value ?: fallbackCounts
                        ProfileOperationResult.Success(resolved)
                    } else {
                        countsRes
                    }
                }
            }
        }
    }

    suspend fun getFollowersList(
        ownerUid: String,
        targetUid: String,
        limit: Int = 40,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<List<PublicProfile>> {
        if (ownerUid.isBlank() || targetUid.isBlank()) {
            return ProfileOperationResult.Success(emptyList())
        }
        onAuthenticatedUserChanged(ownerUid)
        val now = clockMs()
        val cached = _snapshot.value.followersLists[targetUid]
        if (!forceRefresh &&
            cached != null &&
            cached.value.requestedLimit >= limit &&
            cached.isFresh(now, LIST_TTL_MS)
        ) {
            return ProfileOperationResult.Success(cached.value.users.take(limit))
        }

        return coalesce(ownerUid, "followers:$targetUid:$limit") { epoch ->
            when (val res = followRepository.getFollowers(targetUid, limit = limit)) {
                is ProfileOperationResult.Success -> {
                    val list = res.data
                    val stamp = clockMs()
                    updateForOwner(ownerUid, epoch) { snap ->
                        val profileEntries = list.associate { it.uid to TimedEntry(it, stamp) }
                        val nextFollowerUids = if (targetUid == ownerUid) {
                            val merged = (snap.myFollowerUids?.value ?: emptySet()) + list.map { it.uid }
                            TimedEntry(
                                value = if (list.size < limit) list.map { it.uid }.toSet() else merged,
                                updatedAtMs = stamp
                            )
                        } else {
                            snap.myFollowerUids
                        }
                        // If the complete follower dataset fits under `limit`, we also know exact followerCount!
                        val nextCounts = if (list.size < limit) {
                            val prev = snap.followCounts[targetUid]?.value
                            if (prev != null) {
                                snap.followCounts + (
                                    targetUid to TimedEntry(
                                        prev.copy(followerCount = list.size),
                                        stamp,
                                        isAuthoritative = snap.followCounts[targetUid]?.isAuthoritative == true
                                    )
                                )
                            } else {
                                snap.followCounts
                            }
                        } else {
                            snap.followCounts
                        }
                        snap.copy(
                            profiles = snap.profiles + profileEntries,
                            followersLists = snap.followersLists + (
                                targetUid to TimedEntry(
                                    PagedUsersEntry(users = list, requestedLimit = limit),
                                    stamp
                                )
                            ),
                            myFollowerUids = nextFollowerUids,
                            followCounts = nextCounts
                        )
                    }
                    res
                }
                is ProfileOperationResult.Error -> res
            }
        }
    }

    suspend fun getFollowingList(
        ownerUid: String,
        targetUid: String,
        limit: Int = 40,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<List<PublicProfile>> {
        if (ownerUid.isBlank() || targetUid.isBlank()) {
            return ProfileOperationResult.Success(emptyList())
        }
        onAuthenticatedUserChanged(ownerUid)
        val now = clockMs()
        val cached = _snapshot.value.followingLists[targetUid]
        if (!forceRefresh &&
            cached != null &&
            cached.value.requestedLimit >= limit &&
            cached.isFresh(now, LIST_TTL_MS)
        ) {
            return ProfileOperationResult.Success(cached.value.users.take(limit))
        }

        return coalesce(ownerUid, "following:$targetUid:$limit") { epoch ->
            when (val res = followRepository.getFollowing(targetUid, limit = limit)) {
                is ProfileOperationResult.Success -> {
                    val list = res.data
                    val stamp = clockMs()
                    updateForOwner(ownerUid, epoch) { snap ->
                        val profileEntries = list.associate { it.uid to TimedEntry(it, stamp) }
                        val nextFollowedUids = if (targetUid == ownerUid) {
                            val merged = (snap.myFollowedUids?.value ?: emptySet()) + list.map { it.uid }
                            TimedEntry(
                                value = if (list.size < limit) list.map { it.uid }.toSet() else merged,
                                updatedAtMs = stamp
                            )
                        } else {
                            snap.myFollowedUids
                        }
                        val nextCounts = if (list.size < limit) {
                            val prev = snap.followCounts[targetUid]?.value
                            if (prev != null) {
                                snap.followCounts + (
                                    targetUid to TimedEntry(
                                        prev.copy(followingCount = list.size),
                                        stamp,
                                        isAuthoritative = snap.followCounts[targetUid]?.isAuthoritative == true
                                    )
                                )
                            } else {
                                snap.followCounts
                            }
                        } else {
                            snap.followCounts
                        }
                        snap.copy(
                            profiles = snap.profiles + profileEntries,
                            followingLists = snap.followingLists + (
                                targetUid to TimedEntry(
                                    PagedUsersEntry(users = list, requestedLimit = limit),
                                    stamp
                                )
                            ),
                            myFollowedUids = nextFollowedUids,
                            followCounts = nextCounts
                        )
                    }
                    res
                }
                is ProfileOperationResult.Error -> res
            }
        }
    }

    suspend fun getBlockedUserIds(
        ownerUid: String,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<Set<String>> {
        if (ownerUid.isBlank()) return ProfileOperationResult.Success(emptySet())
        onAuthenticatedUserChanged(ownerUid)
        val now = clockMs()
        val cached = _snapshot.value.myBlockedUids
        if (!forceRefresh && cached != null && cached.isFresh(now, LIST_TTL_MS)) {
            return ProfileOperationResult.Success(cached.value)
        }

        return coalesce(ownerUid, "blocked:$ownerUid") { epoch ->
            when (val res = blockRepository.getBlockedUserIds(ownerUid)) {
                is ProfileOperationResult.Success -> {
                    val set = res.data.toSet()
                    val stamp = clockMs()
                    updateForOwner(ownerUid, epoch) { snap ->
                        snap.copy(myBlockedUids = TimedEntry(set, stamp))
                    }
                    ProfileOperationResult.Success(set)
                }
                is ProfileOperationResult.Error -> res
            }
        }
    }

    /**
     * Loads the signed-in user's social overview (exact counts, blocked IDs, followers, following)
     * concurrently instead of sequentially.
     */
    suspend fun refreshOwnSocialOverview(
        ownerUid: String,
        listLimit: Int = 40,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<FollowCounts> {
        if (ownerUid.isBlank()) return ProfileOperationResult.Error("UID must not be blank.")
        onAuthenticatedUserChanged(ownerUid)
        return coroutineScope {
            val countsDeferred = async { getFollowCounts(ownerUid, ownerUid, forceRefresh = forceRefresh) }
            val blockedDeferred = async { getBlockedUserIds(ownerUid, forceRefresh = forceRefresh) }
            val followersDeferred = async { getFollowersList(ownerUid, ownerUid, limit = listLimit, forceRefresh = forceRefresh) }
            val followingDeferred = async { getFollowingList(ownerUid, ownerUid, limit = listLimit, forceRefresh = forceRefresh) }

            blockedDeferred.await()
            followersDeferred.await()
            followingDeferred.await()
            countsDeferred.await()
        }
    }

    suspend fun searchProfiles(
        ownerUid: String?,
        rawQuery: String,
        limit: Int = 20,
        forceRefresh: Boolean = false
    ): ProfileOperationResult<List<PublicProfile>> {
        val normalized = UsernameValidator.normalize(rawQuery).removePrefix("@").trim()
        if (normalized.isBlank()) return ProfileOperationResult.Success(emptyList())

        val scopeUid = ownerUid?.trim()?.takeIf { it.isNotBlank() }
        if (scopeUid != null) {
            onAuthenticatedUserChanged(scopeUid)
            val now = clockMs()
            val cached = _snapshot.value.searchResults[normalized]
            if (!forceRefresh && cached != null && cached.isFresh(now, SEARCH_TTL_MS)) {
                return ProfileOperationResult.Success(cached.value)
            }
            return coalesce(scopeUid, "search:$normalized:$limit") { epoch ->
                when (val res = publicProfileRepository.searchPublicProfiles(normalized, scopeUid, limit)) {
                    is ProfileOperationResult.Success -> {
                        val stamp = clockMs()
                        updateForOwner(scopeUid, epoch) { snap ->
                            val profMap = res.data.associate { it.uid to TimedEntry(it, stamp) }
                            snap.copy(
                                profiles = snap.profiles + profMap,
                                searchResults = snap.searchResults + (normalized to TimedEntry(res.data, stamp))
                            )
                        }
                        res
                    }
                    is ProfileOperationResult.Error -> res
                }
            }
        } else {
            return publicProfileRepository.searchPublicProfiles(normalized, null, limit)
        }
    }

    /**
     * Executes Follow or Unfollow with duplicate-tap protection, immediate optimistic state update,
     * and exact rollback on failure.
     *
     * Returns `null` if a mutation for the same `(actorUid, targetUid)` pair is already in progress.
     */
    suspend fun setFollowState(
        actorUid: String,
        targetUid: String,
        wantFollow: Boolean,
        targetProfileHint: PublicProfile? = null
    ): ProfileOperationResult<Boolean>? {
        if (actorUid.isBlank() || targetUid.isBlank() || actorUid == targetUid) {
            return ProfileOperationResult.Error("Invalid follow participants.")
        }
        onAuthenticatedUserChanged(actorUid)
        val mutationKey = "follow:$actorUid:$targetUid"

        val capturedEpoch: Long
        var preActorCounts: TimedEntry<FollowCounts>? = null
        var preTargetCounts: TimedEntry<FollowCounts>? = null
        var preActorProfile: TimedEntry<PublicProfile>? = null
        var preTargetProfile: TimedEntry<PublicProfile>? = null
        var preFollowedSet: TimedEntry<Set<String>>? = null
        var preActorFollowingList: TimedEntry<PagedUsersEntry>? = null
        var preTargetFollowersList: TimedEntry<PagedUsersEntry>? = null
        var preChatRel: TimedEntry<ChatPeerRelationship>? = null

        synchronized(lock) {
            val current = _snapshot.value
            if (current.ownerUid != actorUid) return ProfileOperationResult.Error("Session changed.")
            if (current.activeMutations.contains(mutationKey)) {
                return null // Duplicate tap while in-flight: ignore safely
            }
            capturedEpoch = sessionEpoch
            preActorCounts = current.followCounts[actorUid]
            preTargetCounts = current.followCounts[targetUid]
            preActorProfile = current.profiles[actorUid]
            preTargetProfile = current.profiles[targetUid]
            preFollowedSet = current.myFollowedUids
            preActorFollowingList = current.followingLists[actorUid]
            preTargetFollowersList = current.followersLists[targetUid]
            preChatRel = current.chatRelationships[targetUid]

            val stamp = clockMs()
            val delta = if (wantFollow) 1 else -1

            val nextFollowed = TimedEntry(
                value = if (wantFollow) {
                    (current.myFollowedUids?.value ?: emptySet()) + targetUid
                } else {
                    (current.myFollowedUids?.value ?: emptySet()) - targetUid
                },
                updatedAtMs = stamp
            )

            val updatedCounts = current.followCounts.toMutableMap()
            preActorCounts?.let { actorEntry ->
                updatedCounts[actorUid] = actorEntry.copy(
                    value = actorEntry.value.copy(
                        followingCount = (actorEntry.value.followingCount + delta).coerceAtLeast(0)
                    ),
                    updatedAtMs = stamp
                )
            }
            preTargetCounts?.let { targetEntry ->
                updatedCounts[targetUid] = targetEntry.copy(
                    value = targetEntry.value.copy(
                        followerCount = (targetEntry.value.followerCount + delta).coerceAtLeast(0)
                    ),
                    updatedAtMs = stamp
                )
            }

            val updatedProfiles = current.profiles.toMutableMap()
            val resolvedTargetProfile = preTargetProfile?.value ?: targetProfileHint
            if (resolvedTargetProfile != null) {
                val baseFollowers = preTargetCounts?.value?.followerCount ?: resolvedTargetProfile.followerCount
                updatedProfiles[targetUid] = TimedEntry(
                    value = resolvedTargetProfile.copy(
                        followerCount = (baseFollowers + delta).coerceAtLeast(0)
                    ),
                    updatedAtMs = stamp
                )
            }
            preActorProfile?.let { actorProf ->
                updatedProfiles[actorUid] = actorProf.copy(
                    value = actorProf.value.copy(
                        followingCount = (actorProf.value.followingCount + delta).coerceAtLeast(0)
                    ),
                    updatedAtMs = stamp
                )
            }

            val updatedFollowingLists = current.followingLists.toMutableMap()
            preActorFollowingList?.let { listEntry ->
                val nextUsers = if (wantFollow) {
                    val profileToInsert = updatedProfiles[targetUid]?.value
                    if (profileToInsert != null && listEntry.value.users.none { it.uid == targetUid }) {
                        listOf(profileToInsert) + listEntry.value.users
                    } else {
                        listEntry.value.users
                    }
                } else {
                    listEntry.value.users.filterNot { it.uid == targetUid }
                }
                updatedFollowingLists[actorUid] = TimedEntry(
                    value = listEntry.value.copy(users = nextUsers),
                    updatedAtMs = stamp
                )
            }

            val updatedChatRels = current.chatRelationships.toMutableMap()
            preChatRel?.let { rel ->
                val peerFollowsActor = current.myFollowerUids?.value?.contains(targetUid)
                if (!wantFollow) {
                    updatedChatRels[targetUid] = TimedEntry(rel.value.copy(isMutual = false), stamp)
                } else if (peerFollowsActor == true) {
                    updatedChatRels[targetUid] = TimedEntry(rel.value.copy(isMutual = true), stamp)
                } else {
                    updatedChatRels.remove(targetUid)
                }
            }

            _snapshot.value = current.copy(
                myFollowedUids = nextFollowed,
                followCounts = updatedCounts,
                profiles = updatedProfiles,
                followingLists = updatedFollowingLists,
                // Invalidate target's followers list timestamp so opening it next fetches fresh order
                followersLists = current.followersLists - targetUid,
                chatRelationships = updatedChatRels,
                activeMutations = current.activeMutations + mutationKey
            )
        }

        try {
            val result = if (wantFollow) {
                followRepository.followUser(actorUid, targetUid)
            } else {
                followRepository.unfollowUser(actorUid, targetUid)
            }

            when (result) {
                is ProfileOperationResult.Success -> {
                    val didMutateServer = result.data
                    updateForOwner(actorUid, capturedEpoch) { snap ->
                        // If server reported idempotent no-op (relationship was already in desired state),
                        // restore pre-mutation counts so local counts never double-increment/decrement.
                        val finalCounts = if (!didMutateServer) {
                            val restored = snap.followCounts.toMutableMap()
                            val savedActorCounts = preActorCounts
                            val savedTargetCounts = preTargetCounts
                            if (savedActorCounts != null) restored[actorUid] = savedActorCounts else restored.remove(actorUid)
                            if (savedTargetCounts != null) restored[targetUid] = savedTargetCounts else restored.remove(targetUid)
                            restored
                        } else {
                            snap.followCounts
                        }
                        snap.copy(
                            followCounts = finalCounts,
                            activeMutations = snap.activeMutations - mutationKey
                        )
                    }
                    return result
                }
                is ProfileOperationResult.Error -> {
                    rollbackFollowMutation(
                        actorUid = actorUid,
                        targetUid = targetUid,
                        capturedEpoch = capturedEpoch,
                        mutationKey = mutationKey,
                        preActorCounts = preActorCounts,
                        preTargetCounts = preTargetCounts,
                        preActorProfile = preActorProfile,
                        preTargetProfile = preTargetProfile,
                        preFollowedSet = preFollowedSet,
                        preActorFollowingList = preActorFollowingList,
                        preTargetFollowersList = preTargetFollowersList,
                        preChatRel = preChatRel
                    )
                    return result
                }
            }
        } catch (ce: CancellationException) {
            updateForOwner(actorUid, capturedEpoch) { snap ->
                snap.copy(activeMutations = snap.activeMutations - mutationKey)
            }
            throw ce
        } catch (e: Exception) {
            rollbackFollowMutation(
                actorUid = actorUid,
                targetUid = targetUid,
                capturedEpoch = capturedEpoch,
                mutationKey = mutationKey,
                preActorCounts = preActorCounts,
                preTargetCounts = preTargetCounts,
                preActorProfile = preActorProfile,
                preTargetProfile = preTargetProfile,
                preFollowedSet = preFollowedSet,
                preActorFollowingList = preActorFollowingList,
                preTargetFollowersList = preTargetFollowersList,
                preChatRel = preChatRel
            )
            return ProfileOperationResult.Error(e.message ?: "Follow action failed", cause = e)
        }
    }

    private fun rollbackFollowMutation(
        actorUid: String,
        targetUid: String,
        capturedEpoch: Long,
        mutationKey: String,
        preActorCounts: TimedEntry<FollowCounts>?,
        preTargetCounts: TimedEntry<FollowCounts>?,
        preActorProfile: TimedEntry<PublicProfile>?,
        preTargetProfile: TimedEntry<PublicProfile>?,
        preFollowedSet: TimedEntry<Set<String>>?,
        preActorFollowingList: TimedEntry<PagedUsersEntry>?,
        preTargetFollowersList: TimedEntry<PagedUsersEntry>?,
        preChatRel: TimedEntry<ChatPeerRelationship>?
    ) {
        updateForOwner(actorUid, capturedEpoch) { snap ->
            val counts = snap.followCounts.toMutableMap()
            if (preActorCounts != null) counts[actorUid] = preActorCounts else counts.remove(actorUid)
            if (preTargetCounts != null) counts[targetUid] = preTargetCounts else counts.remove(targetUid)

            val profiles = snap.profiles.toMutableMap()
            if (preActorProfile != null) profiles[actorUid] = preActorProfile else profiles.remove(actorUid)
            if (preTargetProfile != null) profiles[targetUid] = preTargetProfile else profiles.remove(targetUid)

            val followingLists = snap.followingLists.toMutableMap()
            if (preActorFollowingList != null) {
                followingLists[actorUid] = preActorFollowingList
            } else {
                followingLists.remove(actorUid)
            }

            val followersLists = snap.followersLists.toMutableMap()
            if (preTargetFollowersList != null) {
                followersLists[targetUid] = preTargetFollowersList
            }

            val chatRels = snap.chatRelationships.toMutableMap()
            if (preChatRel != null) chatRels[targetUid] = preChatRel else chatRels.remove(targetUid)

            snap.copy(
                myFollowedUids = preFollowedSet,
                followCounts = counts,
                profiles = profiles,
                followingLists = followingLists,
                followersLists = followersLists,
                chatRelationships = chatRels,
                activeMutations = snap.activeMutations - mutationKey
            )
        }
    }

    suspend fun loadChatPeerState(
        ownerUid: String,
        friendUid: String,
        forceRefresh: Boolean = false
    ): ChatPeerRelationship {
        onAuthenticatedUserChanged(ownerUid)
        val now = clockMs()
        val cached = _snapshot.value.chatRelationships[friendUid]
        if (!forceRefresh && cached != null && cached.isFresh(now, CHAT_REL_TTL_MS)) {
            return cached.value
        }

        return coalesce(ownerUid, "chatRel:$friendUid") { epoch ->
            coroutineScope {
                val blockedByMeDef = async { blockRepository.isBlocked(ownerUid, friendUid) }
                val blockedByPeerDef = async { blockRepository.isBlocked(friendUid, ownerUid) }
                val profileDef = async { getPublicProfile(ownerUid, friendUid, forceRefresh = forceRefresh) }

                val blockedByMe = blockedByMeDef.await()
                val blockedByPeer = blockedByPeerDef.await()
                val mutual = if (!blockedByMe && !blockedByPeer) {
                    followRepository.isMutual(ownerUid, friendUid)
                } else {
                    false
                }
                profileDef.await()

                val rel = ChatPeerRelationship(
                    friendUid = friendUid,
                    isMutual = mutual,
                    isBlockedByMe = blockedByMe,
                    isBlockedByPeer = blockedByPeer
                )
                val stamp = clockMs()
                updateForOwner(ownerUid, epoch) { snap ->
                    val nextBlocked = if (blockedByMe) {
                        TimedEntry((snap.myBlockedUids?.value ?: emptySet()) + friendUid, stamp)
                    } else {
                        snap.myBlockedUids?.let { TimedEntry(it.value - friendUid, stamp) }
                    }
                    snap.copy(
                        chatRelationships = snap.chatRelationships + (friendUid to TimedEntry(rel, stamp)),
                        myBlockedUids = nextBlocked
                    )
                }
                rel
            }
        }
    }

    fun cacheConversationMessages(
        ownerUid: String,
        conversationId: String,
        messages: List<FirestoreChatMessage>
    ) {
        if (ownerUid.isBlank() || conversationId.isBlank()) return
        onAuthenticatedUserChanged(ownerUid)
        updateForOwner(ownerUid) { snap ->
            snap.copy(chatMessages = snap.chatMessages + (conversationId to messages))
        }
    }

    fun recordBlockSuccess(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        onAuthenticatedUserChanged(blockerUid)
        val stamp = clockMs()
        updateForOwner(blockerUid) { snap ->
            val wasFollowing = snap.myFollowedUids?.value?.contains(blockedUid) == true ||
                snap.followingLists[blockerUid]?.value?.users?.any { it.uid == blockedUid } == true
            val wasFollower = snap.myFollowerUids?.value?.contains(blockedUid) == true ||
                snap.followersLists[blockerUid]?.value?.users?.any { it.uid == blockedUid } == true

            val nextCounts = snap.followCounts.toMutableMap()
            snap.followCounts[blockerUid]?.let { entry ->
                nextCounts[blockerUid] = TimedEntry(
                    value = FollowCounts(
                        followerCount = (entry.value.followerCount - if (wasFollower) 1 else 0).coerceAtLeast(0),
                        followingCount = (entry.value.followingCount - if (wasFollowing) 1 else 0).coerceAtLeast(0)
                    ),
                    updatedAtMs = stamp,
                    isAuthoritative = false
                )
            }
            nextCounts.remove(blockedUid)

            val nextFollowingLists = snap.followingLists.toMutableMap()
            snap.followingLists[blockerUid]?.let { entry ->
                nextFollowingLists[blockerUid] = TimedEntry(
                    entry.value.copy(users = entry.value.users.filterNot { it.uid == blockedUid }),
                    stamp
                )
            }
            val nextFollowersLists = snap.followersLists.toMutableMap()
            snap.followersLists[blockerUid]?.let { entry ->
                nextFollowersLists[blockerUid] = TimedEntry(
                    entry.value.copy(users = entry.value.users.filterNot { it.uid == blockedUid }),
                    stamp
                )
            }

            snap.copy(
                myBlockedUids = TimedEntry((snap.myBlockedUids?.value ?: emptySet()) + blockedUid, stamp),
                myFollowedUids = snap.myFollowedUids?.let { TimedEntry(it.value - blockedUid, stamp) },
                myFollowerUids = snap.myFollowerUids?.let { TimedEntry(it.value - blockedUid, stamp) },
                followCounts = nextCounts,
                followingLists = nextFollowingLists,
                followersLists = nextFollowersLists,
                chatRelationships = snap.chatRelationships + (
                    blockedUid to TimedEntry(
                        ChatPeerRelationship(
                            friendUid = blockedUid,
                            isMutual = false,
                            isBlockedByMe = true,
                            isBlockedByPeer = snap.chatRelationships[blockedUid]?.value?.isBlockedByPeer ?: false
                        ),
                        stamp
                    )
                )
            )
        }
    }

    fun recordUnblockSuccess(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        onAuthenticatedUserChanged(blockerUid)
        val stamp = clockMs()
        updateForOwner(blockerUid) { snap ->
            snap.copy(
                myBlockedUids = snap.myBlockedUids?.let { TimedEntry(it.value - blockedUid, stamp) },
                chatRelationships = snap.chatRelationships - blockedUid
            )
        }
    }
}

/**
 * Shared singleton social session cache wired to WorkDaily's production Firestore repositories.
 */
object SocialSessionCache {
    val publicProfileRepository: FirestorePublicProfileRepository by lazy { FirestorePublicProfileRepository() }
    val followRepository: FirestoreFollowRepository by lazy { FirestoreFollowRepository() }
    val blockRepository: FirestoreBlockRepository by lazy { FirestoreBlockRepository() }
    val chatRepository: FirestoreChatRepository by lazy {
        FirestoreChatRepository(
            followRepository = followRepository,
            blockRepository = blockRepository
        )
    }

    val manager: SocialSessionManager by lazy {
        SocialSessionManager(
            publicProfileRepository = publicProfileRepository,
            followRepository = followRepository,
            blockRepository = blockRepository,
            onClearExternalCaches = { chatRepository.clearVerifiedCache() }
        )
    }

    fun onAuthenticatedUserChanged(uid: String?) {
        manager.onAuthenticatedUserChanged(uid)
    }

    fun clear() {
        manager.clear()
    }
}

/**
 * Reactively observes the currently authenticated Firebase UID via [FirebaseAuth.AuthStateListener]
 * and keeps [SocialSessionCache] strictly isolated to that UID.
 */
@Composable
fun rememberCurrentFirebaseUid(): State<String?> {
    val uidState = remember {
        val initialUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Throwable) {
            null
        }
        SocialSessionCache.onAuthenticatedUserChanged(initialUid)
        mutableStateOf(initialUid)
    }

    DisposableEffect(Unit) {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (_: Throwable) {
            null
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val nextUid = firebaseAuth.currentUser?.uid
            SocialSessionCache.onAuthenticatedUserChanged(nextUid)
            uidState.value = nextUid
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    return uidState
}
