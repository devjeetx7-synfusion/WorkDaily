package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.model.LiveMemberRole
import com.synfusion.attendance.live.model.LiveRoomMember

/**
 * Presence rules for the Live room viewer list.
 *
 * DOCUMENTED COUNTING CONVENTION
 * ------------------------------
 * `viewerCount` is the number of **active unique participants excluding the host**, matching the
 * existing WorkDaily Live UI where the broadcaster is displayed separately (own header chip and a
 * dedicated `HOST` row in the viewer sheet). The host is always listed first in the sheet.
 *
 * A member is *active* when its heartbeat is no older than [ACTIVE_WINDOW_MILLIS]. Members are
 * written with `uid` as document id, so reconnecting can never create a duplicate row; stale rows
 * from a crashed device drop out of the count automatically and are pruned by the client.
 */
object PresencePolicy {

    /** Three missed heartbeats at [WatchPartySyncPolicy.PRESENCE_HEARTBEAT_INTERVAL_MILLIS]. */
    const val ACTIVE_WINDOW_MILLIS = 75_000L

    /** Maximum number of member rows rendered at once (Firestore query limit). */
    const val MEMBER_QUERY_LIMIT = 200L

    /**
     * Deduplicates members by UID, keeping the freshest heartbeat, then orders:
     * host first, then moderators, then by join time.
     */
    fun merge(
        previous: List<LiveRoomMember>,
        incoming: List<LiveRoomMember>
    ): List<LiveRoomMember> {
        val byUid = LinkedHashMap<String, LiveRoomMember>(previous.size + incoming.size)
        previous.forEach { byUid[it.uid] = it }
        incoming.forEach { member ->
            val existing = byUid[member.uid]
            byUid[member.uid] = when {
                existing == null -> member
                existing.lastSeenAtMillis <= member.lastSeenAtMillis -> member
                else -> existing
            }
        }
        return sort(byUid.values.toList())
    }

    fun sort(members: Collection<LiveRoomMember>): List<LiveRoomMember> = members.sortedWith(
        compareBy(
            { if (it.role == LiveMemberRole.HOST) 0 else if (it.role == LiveMemberRole.MODERATOR) 1 else 2 },
            { it.joinedAtMillis },
            { it.displayName.lowercase() }
        )
    )

    /** Members whose heartbeat is still inside the active window (host always included). */
    fun activeMembers(
        members: List<LiveRoomMember>,
        nowMillis: Long,
        windowMillis: Long = ACTIVE_WINDOW_MILLIS
    ): List<LiveRoomMember> = sort(
        members.filter { member ->
            member.isHost || (member.lastSeenAtMillis > 0L && nowMillis - member.lastSeenAtMillis <= windowMillis)
        }
    )

    /** Same as [activeMembers] but tolerates a small negative clock skew. */
    fun countActiveViewers(
        members: List<LiveRoomMember>,
        nowMillis: Long,
        windowMillis: Long = ACTIVE_WINDOW_MILLIS
    ): Int = activeMembers(members, nowMillis, windowMillis).count { !it.isHost }

    fun activeParticipantCount(
        members: List<LiveRoomMember>,
        nowMillis: Long,
        windowMillis: Long = ACTIVE_WINDOW_MILLIS
    ): Int = activeMembers(members, nowMillis, windowMillis).size

    /** UIDs that should be pruned from the members collection. */
    fun staleMemberUids(
        members: List<LiveRoomMember>,
        nowMillis: Long,
        windowMillis: Long = ACTIVE_WINDOW_MILLIS
    ): List<String> = members.filter { member ->
        !member.isHost && (member.lastSeenAtMillis <= 0L || nowMillis - member.lastSeenAtMillis > windowMillis)
    }.map { it.uid }

    /** Heartbeat is due when the last write is older than the configured interval. */
    fun isHeartbeatDue(lastHeartbeatMillis: Long, nowMillis: Long): Boolean =
        lastHeartbeatMillis <= 0L ||
            nowMillis - lastHeartbeatMillis >= WatchPartySyncPolicy.PRESENCE_HEARTBEAT_INTERVAL_MILLIS

    /** Only the host may remove another participant from the room. */
    fun canModerate(actorUid: String?, roomHostUid: String?, targetUid: String?): Boolean =
        !actorUid.isNullOrBlank() &&
            actorUid == roomHostUid &&
            !targetUid.isNullOrBlank() &&
            targetUid != roomHostUid
}
