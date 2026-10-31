package com.synfusion.attendance.live.media

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.synfusion.attendance.online.awaitTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Values returned by the token endpoint; tokens must never be logged or persisted. */
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

/**
 * LiveKit Token Client interacting with the Cloudflare Worker token-generation endpoint.
 * Authenticates requests using the user's verified Firebase Auth ID token.
 */
class LiveKitTokenClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val endpointUrl: String = DEFAULT_WORKER_URL,
    private val urlConnectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection }
) {
    companion object {
        /**
         * Cloudflare Worker LiveKit token generation endpoint.
         * Can be overridden via constructor or updated with the deployed worker URL.
         */
        const val DEFAULT_WORKER_URL = "https://workdaily-livekit-token.workdaily.workers.dev/liveKitToken"
        private val gson = Gson()
    }

    suspend fun request(roomId: String): LiveKitCredentials {
        require(Regex("^[A-Za-z0-9_-]{1,128}$").matches(roomId)) { "Invalid room ID" }
        val currentUser = auth.currentUser ?: error("Sign in to join the live stream")
        val uid = currentUser.uid

        // Retrieve fresh Firebase Auth ID token to authenticate against Cloudflare Worker
        val tokenResult = currentUser.getIdToken(false).awaitTask()
        val idToken = tokenResult?.token ?: error("Failed to obtain authentication token.")
        check(auth.currentUser?.uid == uid) { "Signed-in account changed" }

        val responseMap = withContext(Dispatchers.IO) {
            val url = URL(endpointUrl)
            val conn = urlConnectionFactory(url).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $idToken")
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                doInput = true
            }

            try {
                val payload = mapOf("roomId" to roomId)
                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(gson.toJson(payload))
                    writer.flush()
                }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.use { s ->
                    BufferedReader(InputStreamReader(s, "UTF-8")).use { reader ->
                        reader.readText()
                    }
                }.orEmpty()

                if (responseCode !in 200..299) {
                    val parsedError = runCatching {
                        gson.fromJson(responseText, Map::class.java)["error"] as? String
                    }.getOrNull()
                    error(parsedError ?: "Failed to join live room ($responseCode)")
                }

                @Suppress("UNCHECKED_CAST")
                gson.fromJson(responseText, Map::class.java) as Map<String, Any?>
            } finally {
                conn.disconnect()
            }
        }

        check(auth.currentUser?.uid == uid) { "Signed-in account changed" }
        return parseLiveKitCredentials(responseMap, roomId)
    }
}
