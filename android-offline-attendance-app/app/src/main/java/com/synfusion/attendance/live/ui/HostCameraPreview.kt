package com.synfusion.attendance.live.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.livekit.android.LiveKit
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import livekit.org.webrtc.RendererCommon

/** Local preview only: this never connects, starts a room, or records microphone audio. */
@Composable
internal fun HostCameraPreview() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var requested by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var track by remember { mutableStateOf<LocalVideoTrack?>(null) }
    var renderer by remember { mutableStateOf<TextureViewRenderer?>(null) }
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        requested = granted && foreground
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (!foreground) requested = false
            if (foreground && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requested = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(requested, foreground) {
        var room: Room? = null
        var capture: LocalVideoTrack? = null
        var surface: TextureViewRenderer? = null
        if (requested && foreground && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val activeRoom = LiveKit.create(context.applicationContext)
                room = activeRoom
                val video = activeRoom.localParticipant.createVideoTrack()
                capture = video
                val view = TextureViewRenderer(context)
                surface = view
                activeRoom.initVideoRenderer(view)
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                view.setMirror(true)
                video.addRenderer(view)
                video.startCapture()
                track = video
                renderer = view
                error = null
            } catch (_: Exception) {
                error = "Camera could not be started. Check permission and try again."
                surface?.let { view -> capture?.removeRenderer(view); view.release() }
                capture?.let { runCatching { it.stopCapture() }; runCatching { it.dispose() } }
                room?.let { runCatching { it.release() } }
                capture = null; surface = null; room = null
                track = null; renderer = null; requested = false
            }
        }
        onDispose {
            renderer = null; track = null
            surface?.let { view -> capture?.removeRenderer(view); view.release() }
            capture?.let { runCatching { it.stopCapture() }; runCatching { it.dispose() } }
            room?.let { runCatching { it.release() } }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val activeView = renderer
        val activeTrack = track
        if (activeView != null && activeTrack != null) {
            AndroidView(factory = { activeView }, modifier = Modifier.fillMaxWidth().height(180.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val next = if (activeTrack.options.position == CameraPosition.FRONT) CameraPosition.BACK else CameraPosition.FRONT
                    runCatching { activeTrack.switchCamera(position = next); activeView.setMirror(next == CameraPosition.FRONT) }
                        .onFailure { error = "Could not switch cameras." }
                }) { Text("Switch camera") }
                OutlinedButton(onClick = { requested = false }) { Text("Stop preview") }
            }
        } else {
            OutlinedButton(onClick = {
                error = null
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    denied = false; requested = true
                } else launcher.launch(Manifest.permission.CAMERA)
            }) { Text("Preview camera") }
        }
        if (denied) {
            Text("Camera access is needed for preview. You can enable it in app settings.")
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text("Open settings") }
        }
        error?.let { Text(it) }
    }
}
