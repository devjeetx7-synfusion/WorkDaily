# Live media: partial Android foundation (not a streaming implementation)

The viewer screen now calls the token service and attempts a LiveKit subscriber connection for an active persisted video/audio room. It renders the host camera track in video rooms and LiveKit handles subscribed audio. This path is not compiled or integration-tested. **Host broadcasting is not implemented**, so the app cannot yet originate such a stream. The video setup now offers a permission-gated local camera preview, but does not capture microphone audio or publish. Do not deploy or advertise this as working live streaming. In particular, the existing create-room operation sets `isLive` immediately, before any media is connected; new video/audio room creation is blocked in the ViewModel to avoid advertising a nonexistent broadcast. This guard is client-side only; other clients can still create a Firestore room as live. Existing stale rooms are not automatically repaired. Text and Watch Party creation remain unchanged.

## Audit and security boundaries

- Android: compile/target SDK 36, min SDK 24, Kotlin 1.9.0, AGP 8.1.1, Compose 1.5.1. LiveKit Android 2.5.0 and a preliminary media adapter are present. Viewer connection is wired to active room UI; host camera permission and local preview are present but microphone permission and host publishing UI integration are not implemented. Placeholder setup media controls have been removed. No media foreground service was found.
- `FirestoreLiveRoomBackend` creates `live_rooms` and marks them live immediately. `LiveRoomSession` manages membership, heartbeat, chat and Watch Party independently of media. A query-before-create check is not an atomic uniqueness guarantee, and partial creation can leave an active room behind. The existing presence policy excludes stale members but does not reliably end stale rooms after process death.
- Firestore rules restrict room updates to the persisted host, but all authenticated users can read room documents and self-create viewer membership. Consequently, neither private room metadata nor private-room membership is an invitation/authorization mechanism. The new token endpoint denies private-room viewers entirely until a trusted invitation mechanism exists; it also denies either-direction blocks. The updated, not-yet-deployed Firestore rules reserve mediaManaged room creation and modification to trusted backend code, but do not enforce the media-only privacy policy on chat/membership.
- Text rooms and YouTube Watch Party are independent of the absent media transport. The Live screens have been changed for the viewer path and disabled host media setup; Watch Party code is unchanged.

## Token endpoint

`functions/index.js` exports `liveKitToken`, a Firebase Functions v2 callable. Callable authentication validates the Firebase ID token; its UID, not client-supplied identity or role, is used. The server reads the room and block records with Admin SDK privileges. For server-owned, unexpired PREPARING reservations with a matching trusted host lock, only the persisted host can receive a publishing token. Viewers may receive a token only when the room is live and non-ended. The host gets a room-scoped publishing grant; other users get subscribe-only grants for public, unblocked rooms. Data publication and metadata updates are denied. Tokens expire after five minutes for new connections and use `workdaily-<Firestore room id>` as the LiveKit room name. The server URL must use WSS. The response contains only URL, token, room name and TTL.

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
firebase deploy --config firebase.json --only functions --project YOUR_FIREBASE_PROJECT_ID
```

Enter secret values through the Firebase CLI prompts. This separate Functions config does not deploy Firestore rules. Deploy the updated repository `firestore.rules` through the project's normal rules deployment process. Deploy the rules before the Functions and enable the reserved-room flow only after both are deployed and tested. Do not assume private rooms are secure merely because their media token is denied. Verify deployed callable region and ensure Android is configured for the same Firebase project/region. Diagnose authentication, room status, public access, block records, secret configuration, WSS reachability and LiveKit server logs without logging tokens or secrets. Missing server settings fail closed; unexpected backend failures are returned as generic errors.

## Required integration before release

1. Compile and test the new LiveKit Android 2.5.0 adapter and callable client, then complete their host integration with the existing screens and session lifecycle. The SDK uses a JitPack AudioSwitch dependency. The adapter creates/publishes real tracks when called, but host publishing is not called by the app and the adapter has not been compiled or integration-tested. Implement runtime permission requests with rationale/settings handling only for host capture; viewers need neither. Recheck grants on foreground/revocation and release capture on background. The adapter currently checks grants before enabling capture but does not manage the activity lifecycle.
2. Integrate the new server-owned reservation and activation endpoints with Android. The backend reservation is not discoverable (`isLive=false`); the token endpoint permits only its owner during its ten-minute PREPARING window. Activation checks the LiveKit participant and required unmuted tracks before marking the room LIVE. End marks the room ended before attempting to delete the LiveKit room. The Android host does not use these endpoints yet; retry/rollback, process death handling and stale-live reconciliation remain incomplete.
3. Render actual host/local and remote video, let LiveKit play subscribed audio, implement actual camera/mic toggles and switching, track connection/reconnection states, and close media on end/leave. Add explicit lifecycle/background policy (no background capture unless a compliant foreground service is implemented) and clean up renderers and jobs. Preserve the Firebase session for chat, gifts, presence and Watch Party.
4. Implement trusted private-room invitations, host-ended LiveKit room closure, stale room reconciliation, reauthorization, and rules/emulator tests. The new service intentionally does not provide these.
5. Run Android build, unit tests and lint with JDK 17, Android SDK 36, Firebase configuration and dependency access. Run token integration and rules tests against emulators and deployed staging. No current unit test proves real media transmission.

## Two-device acceptance test (not performed)

With a deployed staging backend, configured LiveKit and a completed Android integration: sign in on two devices as different users. On A grant permissions, preview and start video; on B join and confirm A's real video and audio. Switch A's camera, disable/re-enable it while confirming B still hears audio, then mute/unmute while video continues. Confirm B cannot publish via its token. Verify chat, profiles, gifts, viewer count and Watch Party separately. Interrupt and restore networking, background/foreground and rotate where supported, test denied/revoked permissions and incoming calls, then end on A and verify B transitions to ended and both devices release resources. Test unauthorized, private, blocked, expired and ended joins as well. Record actual device, OS, deployment and results; no such verification was available here.

## Validation for viewer integration

The token-service and lifecycle-policy Node tests passed locally (15 tests) and npm audit found no vulnerabilities. Android compilation, the new policy tests and lint could not run because Java and the Android SDK were unavailable. No Firebase/LiveKit integration or device test was performed. Subscriber lifecycle, rendering and reconnection therefore remain unverified. There is no in-room retry action or audio-only connection indicator yet.

## Server-owned lifecycle foundation (not deployed or wired to Android)

`reserveLiveMediaRoom` creates an initially non-public, not-live document and uses a per-host Firestore lock and returns an existing active reservation on retry. Expired PREPARING reservations are marked FAILED when that host reserves again. `activateLiveMediaRoom` checks the authenticated host and LiveKit participant identity, ACTIVE state, and unmuted microphone (plus camera for VIDEO), then rechecks the reservation in a Firestore transaction. `endLiveMediaRoom` marks the room ENDED and removes the lock before requesting LiveKit room deletion; a failed deletion is returned for retry, and an already-absent LiveKit room is accepted. Firestore rules disallow client creation or modification of mediaManaged rooms.

These endpoints have unit tests with sequential fakes, not Firestore emulator/concurrency tests or a deployed LiveKit test. They do **not** yet provide scheduled stale-room cleanup, reliable retry after process death, protection against legacy client-created live rooms, private-room invitations, or continuous enforcement after token issuance. The activation check and Firestore transition are separated by a network call and are not an atomic proof that a track remains available afterward. The host creation guard remains enabled. Local camera preview does not publish and stops on background/exit.
