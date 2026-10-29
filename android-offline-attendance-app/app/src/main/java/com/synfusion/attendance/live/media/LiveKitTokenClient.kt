package com.synfusion.attendance.live.media

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.synfusion.attendance.online.awaitTask
import java.net.URI

/** Values returned by the callable; tokens must never be logged or persisted. */
class LiveKitCredentials(val url: String, val token: String, val roomName: String)

internal fun parseLiveKitCredentials(data: Any?, roomId: String): LiveKitCredentials {
    val fields = data as? Map<*, *> ?: error("Invalid streaming response")
    val url = fields["url"] as? String ?: error("Invalid streaming response")
    val token = fields["token"] as? String ?: error("Invalid streaming response")
    val name = fields["roomName"] as? String ?: error("Invalid streaming response")
    val uri = runCatching { URI(url) }.getOrNull()
    require(uri?.scheme == "wss" && !uri.host.isNullOrBlank() && uri.userInfo == null)
    require(token.isNotBlank() && name == "workdaily-$roomId")
    return LiveKitCredentials(url, token, name)
}

/** Firebase callable supplies the Firebase ID token automatically; the server chooses the grant. */
class LiveKitTokenClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun request(roomId: String): LiveKitCredentials {
        require(Regex("^[A-Za-z0-9_-]{1,128}$").matches(roomId)) { "Invalid room ID" }
        val uid = auth.currentUser?.uid ?: error("Sign in to join the live stream")
        val result = functions.getHttpsCallable("liveKitToken")
            .call(mapOf("roomId" to roomId)).awaitTask()
        check(auth.currentUser?.uid == uid) { "Signed-in account changed" }
        return parseLiveKitCredentials(result.data, roomId)
    }
}
