package com.synfusion.attendance.live.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.CameraPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** Transport state only; Firestore remains the source of room lifecycle and membership. */
enum class MediaConnection { IDLE, CONNECTING, CONNECTED, RECONNECTING, FAILED }
data class LiveMediaState(
    val connection: MediaConnection = MediaConnection.IDLE,
    val isHost: Boolean = false,
    val cameraEnabled: Boolean = false,
    val microphoneEnabled: Boolean = false,
    val video: VideoTrack? = null,
    val message: String? = null
)

/**
 * One LiveKit room at a time. This adapter is deliberately not wired to room creation yet:
 * the Firestore lifecycle must first support a non-public reservation and rollback.
 * Callers must own it for the entire media session and call disconnect on leaving/backgrounding.
 */
class LiveKitMediaManager(
    context: Context,
    private val scope: CoroutineScope,
    private val tokens: LiveKitTokenClient = LiveKitTokenClient()
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var room: Room? = null
    private var eventsJob: Job? = null
    private var hostUid: String = ""
    private val mutableState = MutableStateFlow(LiveMediaState())
    val state = mutableState.asStateFlow()

    /** Renderer must only be used while the associated session remains connected. */
    fun roomForVideo(track: VideoTrack): Room? = room?.takeIf { mutableState.value.video === track }

    suspend fun connect(roomId: String, persistedHostUid: String, publishVideo: Boolean, publishAudio: Boolean) {
        mutex.withLock {
            check(room == null) { "A media session is already active" }
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Sign in to join")
            require(persistedHostUid.isNotBlank())
            val isHost = uid == persistedHostUid
            require(isHost || (!publishVideo && !publishAudio)) { "Viewers cannot publish" }
            if (publishVideo) requirePermission(Manifest.permission.CAMERA)
            if (publishAudio) requirePermission(Manifest.permission.RECORD_AUDIO)
            hostUid = persistedHostUid
            mutableState.value = LiveMediaState(connection = MediaConnection.CONNECTING, isHost = isHost)
            var candidate: Room? = null
            try {
                val credentials = withTimeout(15_000) { tokens.request(roomId) }
                check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "Signed-in account changed" }
                val active = LiveKit.create(appContext)
                candidate = active
                room = active
                eventsJob = scope.launch {
                    active.events.collect { event ->
                        if (room !== active) return@collect
                        when (event) {
                            is RoomEvent.Reconnecting -> mutableState.value = mutableState.value.copy(connection = MediaConnection.RECONNECTING)
                            is RoomEvent.Reconnected -> {
                                refreshVideo(active)
                                mutableState.value = mutableState.value.copy(connection = MediaConnection.CONNECTED)
                            }
                            is RoomEvent.Disconnected -> mutableState.value = mutableState.value.copy(
                                connection = MediaConnection.FAILED, video = null,
                                cameraEnabled = false, microphoneEnabled = false,
                                message = "Disconnected from live media."
                            )
                            is RoomEvent.TrackSubscribed, is RoomEvent.TrackUnsubscribed,
                            is RoomEvent.TrackMuted, is RoomEvent.TrackUnmuted,
                            is RoomEvent.ParticipantDisconnected -> refreshVideo(active)
                            else -> Unit
                        }
                    }
                }
                withTimeout(30_000) { active.connect(credentials.url, credentials.token) }
                check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "Signed-in account changed" }
                if (publishVideo) {
                    requirePermission(Manifest.permission.CAMERA)
                    active.localParticipant.setCameraEnabled(true)
                    check(active.localParticipant.isCameraEnabled()) { "Camera publishing failed" }
                }
                if (publishAudio) {
                    requirePermission(Manifest.permission.RECORD_AUDIO)
                    active.localParticipant.setMicrophoneEnabled(true)
                    check(active.localParticipant.isMicrophoneEnabled()) { "Microphone publishing failed" }
                }
                check(active.state == Room.State.CONNECTED) { "Media connection lost" }
                refreshVideo(active)
                mutableState.value = mutableState.value.copy(
                    connection = MediaConnection.CONNECTED,
                    cameraEnabled = active.localParticipant.isCameraEnabled(),
                    microphoneEnabled = active.localParticipant.isMicrophoneEnabled()
                )
            } catch (e: Exception) {
                eventsJob?.cancel(); eventsJob = null; room = null
                candidate?.let { runCatching { it.disconnect() }; runCatching { it.release() } }
                mutableState.value = LiveMediaState(connection = MediaConnection.FAILED, message = "Could not connect live media. Check permissions and try again.")
                if (e is CancellationException) throw e
                throw e
            }
        }
    }

    suspend fun setCameraEnabled(enabled: Boolean) = mutex.withLock {
        val active = hostRoom()
        if (enabled) requirePermission(Manifest.permission.CAMERA)
        active.localParticipant.setCameraEnabled(enabled)
        mutableState.value = mutableState.value.copy(cameraEnabled = active.localParticipant.isCameraEnabled())
        refreshVideo(active)
    }

    suspend fun setMicrophoneEnabled(enabled: Boolean) = mutex.withLock {
        val active = hostRoom()
        if (enabled) requirePermission(Manifest.permission.RECORD_AUDIO)
        active.localParticipant.setMicrophoneEnabled(enabled)
        mutableState.value = mutableState.value.copy(microphoneEnabled = active.localParticipant.isMicrophoneEnabled())
    }

    suspend fun switchCamera() = mutex.withLock {
        val track = hostRoom().localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
            ?: error("Camera is not enabled")
        val next = when (track.options.position) {
            CameraPosition.FRONT -> CameraPosition.BACK
            CameraPosition.BACK -> CameraPosition.FRONT
            else -> null
        }
        track.switchCamera(position = next)
    }

    suspend fun disconnect() = mutex.withLock {
        val old = room; room = null
        eventsJob?.cancel(); eventsJob = null
        old?.let { runCatching { it.disconnect() }; runCatching { it.release() } }
        mutableState.value = LiveMediaState()
    }

    private fun hostRoom(): Room {
        check(mutableState.value.isHost && mutableState.value.connection == MediaConnection.CONNECTED &&
            FirebaseAuth.getInstance().currentUser?.uid == hostUid) { "Only a connected host can publish" }
        return checkNotNull(room)
    }

    private fun requirePermission(permission: String) {
        check(ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED) { "Media permission not granted" }
    }

    private fun refreshVideo(active: Room) {
        val participant = if (mutableState.value.isHost) active.localParticipant else
            active.remoteParticipants.values.firstOrNull { it.identity?.value == hostUid }
        val publication = participant?.getTrackPublication(Track.Source.CAMERA)
        val video = if (publication?.muted == false) publication.track as? VideoTrack else null
        mutableState.value = mutableState.value.copy(video = video)
    }
}
