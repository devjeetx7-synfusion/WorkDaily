# Live media: partial Android foundation (not a streaming implementation)

The callable token service and Android media adapter are present, but **the app does not invoke them and does not publish or receive LiveKit media through its UI.** The existing video/audio setup is a visual placeholder, not a camera preview or microphone capture. Do not deploy or advertise this as working live streaming. In particular, the existing create-room operation sets `isLive` immediately, before any media is connected; its “You are LIVE” toast is not evidence of broadcasting.

## Audit and security boundaries

- Android: compile/target SDK 36, min SDK 24, Kotlin 1.9.0, AGP 8.1.1, Compose 1.5.1. LiveKit Android 2.5.0 and a preliminary media adapter have been added, but there is no runtime permission flow or UI integration. Setup camera/mic switches are local Compose state. No media foreground service was found.
- `FirestoreLiveRoomBackend` creates `live_rooms` and marks them live immediately. `LiveRoomSession` manages membership, heartbeat, chat and Watch Party independently of media. A query-before-create check is not an atomic uniqueness guarantee, and partial creation can leave an active room behind. The existing presence policy excludes stale members but does not reliably end stale rooms after process death.
- Firestore rules restrict room updates to the persisted host, but all authenticated users can read room documents and self-create viewer membership. Consequently, neither private room metadata nor private-room membership is an invitation/authorization mechanism. The new token endpoint denies private-room viewers entirely until a trusted invitation mechanism exists; it also denies either-direction blocks. Existing Firestore rules are unchanged and do not enforce this media-only policy on chat/membership.
- Text rooms and YouTube Watch Party are independent of the absent media transport. No existing screens or Watch Party code were changed by this foundation.

## Token endpoint

`functions/index.js` exports `liveKitToken`, a Firebase Functions v2 callable. Callable authentication validates the Firebase ID token; its UID, not client-supplied identity or role, is used. The server reads the room and block records with Admin SDK privileges. Only a live, non-ended room with a host is eligible. The host gets a room-scoped publishing grant; other users get subscribe-only grants for public, unblocked rooms. Data publication and metadata updates are denied. Tokens expire after five minutes for new connections and use `workdaily-<Firestore room id>` as the LiveKit room name. The server URL must use WSS. The response contains only URL, token, room name and TTL.

This authorization is checked at issuance, not continuously. Already issued tokens are not revoked when a room ends, a block is created, or visibility changes; expiry does not by itself disconnect an already connected participant. Production integration must actively remove participants/close the LiveKit room when a host ends the stream, and enforce revocation/visibility changes and stale-room cleanup. A room can change between the authorization read and connection. Firebase callable ID-token verification checks expiration/signature but this code does not check Firebase account revocation. Consider App Check, rate limits and operational monitoring before production. The service is not yet integration-tested against Firebase or LiveKit.

## Configuration and deployment (not performed)

Requires a Firebase project with Auth and Firestore, Blaze/Cloud Functions, Node 22, Firebase CLI, and a LiveKit server reachable by devices over WSS. LiveKit URL is public configuration; API key and especially API secret stay on the server. Never put the secret in Android, source control, logs, or `.env`.

From `functions/`, after selecting the intended Firebase project:

```sh
npm ci
npm test
cp .env.example .env.YOUR_FIREBASE_PROJECT_ID
# Edit that file: set LIVEKIT_URL to your real wss:// LiveKit URL.
firebase functions:secrets:set LIVEKIT_API_KEY --project YOUR_FIREBASE_PROJECT_ID
firebase functions:secrets:set LIVEKIT_API_SECRET --project YOUR_FIREBASE_PROJECT_ID
firebase deploy --config firebase.json --only functions:liveKitToken --project YOUR_FIREBASE_PROJECT_ID
```

Enter secret values through the Firebase CLI prompts. This separate Functions config does not deploy Firestore rules. Deploy the repository's existing `firestore.rules` through the project's normal rules deployment process; no rules changes are included here. Do not assume private rooms are secure merely because their media token is denied. Verify deployed callable region and ensure Android is configured for the same Firebase project/region. Diagnose authentication, room status, public access, block records, secret configuration, WSS reachability and LiveKit server logs without logging tokens or secrets. Missing server settings fail closed; unexpected backend failures are returned as generic errors.

## Required integration before release

1. Compile and test the new LiveKit Android 2.5.0 adapter and callable client, then integrate them with the existing screens and session lifecycle. The SDK uses a JitPack AudioSwitch dependency. The adapter creates/publishes real tracks when called, but is not called by the app and has not been compiled or integration-tested. Implement runtime permission requests with rationale/settings handling only for host capture; viewers need neither. Recheck grants on foreground/revocation and release capture on background. The adapter currently checks grants before enabling capture but does not manage the activity lifecycle.
2. Reserve a room without listing it as live, authorize its host securely, connect and publish real tracks, then atomically make it visible. Roll back or reuse reservations on failure, prevent duplicate starts, and change Firestore rules and tests accordingly. The current token service requires `isLive == true`, so it **cannot** yet implement that safe start sequence.
3. Render actual host/local and remote video, let LiveKit play subscribed audio, implement actual camera/mic toggles and switching, track connection/reconnection states, and close media on end/leave. Add explicit lifecycle/background policy (no background capture unless a compliant foreground service is implemented) and clean up renderers and jobs. Preserve the Firebase session for chat, gifts, presence and Watch Party.
4. Implement trusted private-room invitations, host-ended LiveKit room closure, stale room reconciliation, reauthorization, and rules/emulator tests. The new service intentionally does not provide these.
5. Run Android build, unit tests and lint with JDK 17, Android SDK 36, Firebase configuration and dependency access. Run token integration and rules tests against emulators and deployed staging. No current unit test proves real media transmission.

## Two-device acceptance test (not performed)

With a deployed staging backend, configured LiveKit and a completed Android integration: sign in on two devices as different users. On A grant permissions, preview and start video; on B join and confirm A's real video and audio. Switch A's camera, disable/re-enable it while confirming B still hears audio, then mute/unmute while video continues. Confirm B cannot publish via its token. Verify chat, profiles, gifts, viewer count and Watch Party separately. Interrupt and restore networking, background/foreground and rotate where supported, test denied/revoked permissions and incoming calls, then end on A and verify B transitions to ended and both devices release resources. Test unauthorized, private, blocked, expired and ended joins as well. Record actual device, OS, deployment and results; no such verification was available here.
