# WorkDaily LiveKit Token Backend on Cloudflare Workers Free

This guide explains how to configure, deploy, and verify the dedicated LiveKit token-generation backend using Cloudflare Workers Free.

---

## 1. Overview & Architecture

- **Application:** WorkDaily Android App (`com.synfusion.attendance`)
- **Streaming Engine:** LiveKit Cloud (`wss://<project-domain>.livekit.cloud`)
- **Token Backend:** Cloudflare Worker Free (`cloudflare-livekit-worker/`)
- **Authentication:** Firebase Authentication (User ID Token verification using Google's public JWKS certificates)
- **Database:** Firebase Firestore (verifies room existence, host reservation locks, public status, and room membership)

---

## 2. Prerequisites

1. **Cloudflare Account:** Free account at [cloudflare.com](https://dash.cloudflare.com).
2. **Node.js & npm:** Node.js 18+ or 20+ installed.
3. **LiveKit Cloud Project:** LiveKit Cloud project with credentials:
   - LiveKit WebSocket URL: `wss://<project>.livekit.cloud`
   - LiveKit API Key: `<API_KEY>`
   - LiveKit API Secret: `<API_SECRET>`
4. **Firebase Project:** Your Firebase project ID (e.g., `workdaily-app`).

---

## 3. Worker Configuration

The worker configuration is located in `cloudflare-livekit-worker/wrangler.toml`:

```toml
name = "workdaily-livekit-token"
main = "src/index.js"
compatibility_date = "2024-10-01"
compatibility_flags = ["nodejs_compat"]

[vars]
LIVEKIT_URL = "wss://your-livekit-project.livekit.cloud"
FIREBASE_PROJECT_ID = "your-firebase-project-id"
```

Update `LIVEKIT_URL` and `FIREBASE_PROJECT_ID` in `wrangler.toml` to match your actual services.

---

## 4. Setting Secure Secrets

LiveKit API Key and Secret must NEVER be stored in source code or `wrangler.toml`. Store them as encrypted secrets in Cloudflare Workers:

```bash
cd cloudflare-livekit-worker

# Set LiveKit API Key
npx wrangler secret put LIVEKIT_API_KEY
# Enter your LiveKit API Key when prompted

# Set LiveKit API Secret
npx wrangler secret put LIVEKIT_API_SECRET
# Enter your LiveKit API Secret when prompted
```

---

## 5. Testing the Worker Locally

Run the automated test suite:

```bash
cd cloudflare-livekit-worker
npm test
```

This verifies:
- Correct grant generation (Host gets camera/mic publishing rights; viewers get subscribe-only rights).
- Strict room membership verification for viewers.
- Rejection of private rooms and blocked users.
- Reservation lock enforcement for hosts in PREPARING status.
- Zero credential leakage in responses or errors.

---

## 6. Deploying to Cloudflare Workers

Deploy using Wrangler:

```bash
cd cloudflare-livekit-worker
npx wrangler deploy
```

Upon successful deployment, Cloudflare will print the Worker URL:
```text
Published workdaily-livekit-token (0.42 sec)
  https://workdaily-livekit-token.<your-subdomain>.workers.dev
```

---

## 7. Configuring the Android Application

Update the worker endpoint in `android-offline-attendance-app`:

File: `app/src/main/java/com/synfusion/attendance/live/media/LiveKitTokenClient.kt`

```kotlin
companion object {
    const val DEFAULT_WORKER_URL = "https://workdaily-livekit-token.<your-subdomain>.workers.dev/liveKitToken"
}
```

Or pass `endpointUrl` directly to the `LiveKitTokenClient(endpointUrl = "...")` constructor.

---

## 8. End-to-End Testing & Verification

1. **Host Live Stream:**
   - Sign in to WorkDaily on Device A.
   - Tap **Go Live** and choose **Video** or **Audio**.
   - Grant Camera/Microphone permissions.
   - Tap **Start Live**.
   - The app reserves the room in Firestore, requests a host token from Cloudflare Worker, connects to LiveKit, publishes local tracks, and transitions room status to `LIVE`.

2. **Viewer Join:**
   - Sign in to WorkDaily on Device B.
   - Browse the Live feed and select the broadcast started by Device A.
   - The app joins room membership in Firestore, requests a viewer token from Cloudflare Worker, connects to LiveKit, and renders remote video/audio.

3. **Mute / Camera Toggles & End Stream:**
   - Host toggles microphone or camera from the controls sheet.
   - Host taps **End Live Stream**; tracks are stopped, LiveKit session is disconnected, and the room transitions to `ENDED`.
