package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyState

/**
 * What a participant device should do with a remote Watch Party revision.
 */
sealed class PlaybackSyncDecision {
    /** Apply the remote state to the official player. */
    data class Apply(
        val status: WatchPartyPlaybackStatus,
        val positionSeconds: Float,
        val isOwnEcho: Boolean
    ) : PlaybackSyncDecision()

    /** Nothing to do: same or stale revision. */
    data class Ignore(val reason: String) : PlaybackSyncDecision()
}

/**
 * Pure synchronization policy for the shared Watch Party playback state.
 *
 * The design keeps a single monotonic `revision` per room. Every host action produces exactly one
 * revision, participants apply only increasing revisions, and the host ignores the echo of its own
 * command (matched by [WatchPartyState.commandId]) so a remote snapshot can never bounce the local
 * player back and forth.
 *
 * Position is projected from the revision timestamp instead of a high-frequency position stream:
 * the host publishes play/pause/seek plus a periodic correction, and every device derives the
 * current position locally. This is intentionally *not* frame-perfect.
 */
object WatchPartySyncPolicy {

    /** How often a playing host re-publishes its position so drift stays bounded. */
    const val POSITION_CORRECTION_INTERVAL_MILLIS = 15_000L

    /** Presence heartbeat cadence used while the room is open. */
    const val PRESENCE_HEARTBEAT_INTERVAL_MILLIS = 25_000L

    /** Drift tolerated before a participant is force-seeked. */
    const val SEEK_TOLERANCE_SECONDS = 2.5f

    /** Guards against projecting an absurd position after a long background pause. */
    const val MAX_PROJECTION_SECONDS = 12 * 60 * 60f

    /**
     * Decides whether [remote] must be applied on this device.
     *
     * @param appliedRevision highest revision already applied locally.
     * @param lastLocalCommandId command id of the last command this device published (host echo).
     */
    fun decide(
        remote: WatchPartyState,
        appliedRevision: Long,
        lastLocalCommandId: String?
    ): PlaybackSyncDecision {
        if (remote.revision <= appliedRevision) {
            return PlaybackSyncDecision.Ignore("stale revision ${remote.revision} <= $appliedRevision")
        }
        val isOwnEcho = remote.commandId.isNotBlank() && remote.commandId == lastLocalCommandId
        return PlaybackSyncDecision.Apply(
            status = remote.status,
            positionSeconds = projectedPositionSeconds(remote, System.currentTimeMillis()),
            isOwnEcho = isOwnEcho
        )
    }

    /**
     * Expected playhead for [state] at [nowMillis].
     *
     * Only PLAYING advances, and the elapsed time is clamped so a device that was offline for hours
     * cannot produce an out-of-range seek.
     */
    fun projectedPositionSeconds(state: WatchPartyState, nowMillis: Long): Float {
        if (state.status != WatchPartyPlaybackStatus.PLAYING) return state.positionSeconds.coerceAtLeast(0f)
        val elapsedMillis = (nowMillis - state.effectiveUpdatedAtMillis).coerceAtLeast(0L)
        val elapsedSeconds = elapsedMillis / 1000f
        val projected = state.positionSeconds + elapsedSeconds
        return projected.coerceIn(0f, MAX_PROJECTION_SECONDS)
    }

    /** True when the local playhead drifted far enough to justify a corrective seek. */
    fun shouldCorrectPosition(localPositionSeconds: Float, expectedPositionSeconds: Float): Boolean =
        kotlin.math.abs(localPositionSeconds - expectedPositionSeconds) > SEEK_TOLERANCE_SECONDS

    /** True when a periodic position correction is due. */
    fun isCorrectionDue(lastPublishedAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis - lastPublishedAtMillis >= POSITION_CORRECTION_INTERVAL_MILLIS

    /** Only the persisted host may mutate shared playback state. */
    fun isAuthorizedHost(actorUid: String?, roomHostUid: String?): Boolean =
        !actorUid.isNullOrBlank() && actorUid == roomHostUid

    /**
     * Computes the revision that must be written for [command].
     *
     * The backend transaction re-reads the server revision and rejects the write when it no longer
     * matches [expectedRevision], which keeps revisions strictly increasing even with two devices
     * signed in to the host account.
     */
    fun nextRevision(serverRevision: Long, expectedRevision: Long): Long? {
        if (serverRevision != expectedRevision) return null
        return serverRevision + 1L
    }

    /** Host-side guard: playback commands are rejected once the room is no longer live. */
    fun canAcceptPlaybackCommands(isLive: Boolean, endedAtMillis: Long?): Boolean =
        isLive && endedAtMillis == null

    /**
     * Applies [command] to [current] and returns the state the host should persist.
     *
     * Playlist membership is *not* rewritten here: only the active item ids and playback fields
     * change, so a position update never touches the playlist documents.
     */
    fun applyCommand(
        current: WatchPartyState,
        command: LivePlaybackCommand,
        actorUid: String,
        nowMillis: Long,
        commandId: String
    ): WatchPartyState {
        val base = current.copy(
            revision = current.revision + 1L,
            updatedAtMillis = nowMillis,
            updatedByUserId = actorUid,
            commandId = commandId
        )
        return when (command) {
            is LivePlaybackCommand.Play -> base.copy(
                status = WatchPartyPlaybackStatus.PLAYING,
                positionSeconds = command.positionSeconds.coerceAtLeast(0f),
                isTurnedOff = false
            )

            is LivePlaybackCommand.Pause -> base.copy(
                status = WatchPartyPlaybackStatus.PAUSED,
                positionSeconds = command.positionSeconds.coerceAtLeast(0f)
            )

            is LivePlaybackCommand.Seek -> base.copy(
                status = if (command.resumePlaying) {
                    WatchPartyPlaybackStatus.PLAYING
                } else {
                    WatchPartyPlaybackStatus.PAUSED
                },
                positionSeconds = command.positionSeconds.coerceAtLeast(0f)
            )

            is LivePlaybackCommand.SelectItem -> base.copy(
                videoId = command.videoId,
                playlistId = command.playlistId ?: base.playlistId,
                activePlaylistItemId = command.itemId,
                status = WatchPartyPlaybackStatus.PLAYING,
                positionSeconds = 0f,
                isTurnedOff = false
            )

            is LivePlaybackCommand.TurnOff -> base.copy(
                isTurnedOff = true,
                status = WatchPartyPlaybackStatus.PAUSED,
                commandId = commandId
            )

            is LivePlaybackCommand.TurnOn -> base.copy(
                isTurnedOff = false,
                status = WatchPartyPlaybackStatus.PAUSED,
                positionSeconds = 0f
            )

            is LivePlaybackCommand.SetMuted -> base.copy(isMuted = command.muted)

            is LivePlaybackCommand.Completed -> {
                if (!command.nextItemId.isNullOrBlank() && !command.nextVideoId.isNullOrBlank()) {
                    base.copy(
                        videoId = command.nextVideoId,
                        playlistId = command.nextPlaylistId ?: base.playlistId,
                        activePlaylistItemId = command.nextItemId,
                        status = WatchPartyPlaybackStatus.PLAYING,
                        positionSeconds = 0f
                    )
                } else {
                    base.copy(
                        status = WatchPartyPlaybackStatus.ENDED,
                        positionSeconds = base.positionSeconds
                    )
                }
            }
        }
    }
}
