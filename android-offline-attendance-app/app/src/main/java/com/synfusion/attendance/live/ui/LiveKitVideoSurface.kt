package com.synfusion.attendance.live.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import livekit.org.webrtc.RendererCommon

/** The renderer belongs to exactly one track and is removed before it is released. */
@Composable
internal fun LiveKitVideoSurface(room: Room, track: VideoTrack, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val renderer = remember(room, track) {
        TextureViewRenderer(context).apply {
            room.initVideoRenderer(this)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }
    }
    DisposableEffect(renderer, track) {
        track.addRenderer(renderer)
        onDispose {
            track.removeRenderer(renderer)
            renderer.release()
        }
    }
    AndroidView(factory = { renderer }, modifier = modifier)
}
