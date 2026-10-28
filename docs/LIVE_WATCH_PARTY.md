# WorkDaily Live — Watch Party (implementation notes)

This document describes the Watch Party implementation that extends WorkDaily's existing Live
system. It covers the data model, the Firestore security rules, the YouTube player integration,
presence/chat behavior, the share-link flow, the required Firebase configuration, and exactly what
has and has not been verified.

The official **YouTube embedded player** stays the only video player. No stream extraction, no ad
bypassing, no custom player, no forced fullscreen, and no sample-video fallback.

---

## 1. File map

### New production code

| File | Purpose |
| --- | --- |
| `live/watchparty/YouTubeLinkParser.kt` | Strict URL/ID parsing (host allowlist), player variables, thumbnail URL. |
| `live/watchparty/WatchPartySyncPolicy.kt` | Revision guard, position projection, drift tolerance, host authorization, `LivePlaybackCommand` → new `WatchPartyState`. |
| `live/watchparty/WatchPartyPlaylistLogic.kt` | Add/remove/select/turn on/off, duplicate + capacity rules, active-item reconciliation. |
| `live/watchparty/PresencePolicy.kt` | Member sorting, active-window viewer counting, stale detection, host moderation checks. |
| `live/watchparty/LiveChatPolicy.kt` | Validation, history bounds, optimistic/local merge, pagination cursor. |
| `live/watchparty/LiveShareLink.kt` | `https://workdaily.app/live/{roomId}` + `workdaily://live/{roomId}` build/parse. |
| `live/watchparty/YouTubeOEmbed.kt` | Public oEmbed URL builder + defensive JSON parsing for real titles/thumbnails. |
| `live/model/LiveRoomPresenceModels.kt` | `LiveRoomMember`, `LiveUserIdentity`, `LiveRoomSessionState`, `LivePlaybackCommand`. |
| `live/model/LiveOperationResult.kt` | Generic success/error result with `isAuthError` / `isConflict` flags. |
| `live/data/LiveRoomBackend.kt` | Backend contract + `LiveRoomCreateRequest`. |
| `live/data/LiveRoomSession.kt` | Per-room orchestrator: membership, heartbeat, chat, playback, playlist, notices. |
| `live/data/VideoMetadataProvider.kt` | Real-metadata interface + no-op default. |
| `live/data/InMemoryLiveRoomBackend.kt` | Same contract for JVM tests (deterministic clock, real authorization checks). |
| `online/FirestoreLiveRoomBackend.kt` | Production Firestore/Auth backend. |
| `online/FirebaseTaskAwait.kt` | `Task.awaitTask()` bridge (no extra dependency). |
| `online/YouTubeOEmbedMetadataProvider.kt` | One HTTPS GET to `youtube.com/oembed`, 4 s timeouts, never throws. |
| `live/ui/WatchPartyPlayer.kt` | Draggable rounded stage, official IFrame player in a WebView, hidden/reopen pill, error states. |
| `live/ui/WatchPartyPlaylistSheet.kt` | Playlist sheet: URL composer, rows, playing-now, mute, turn off/on. |
| `live/ui/LiveRoomMembersSheet.kt` | Real viewer/members list (search, follow, host remove). |
| `live/ui/LiveRoomStateScreen.kt` | Loading/unavailable/ended/error states + member profile sheet. |
| `live/ui/LiveDurationFormat.kt` | Real elapsed-time formatting/derivation. |
| `live/ui/LiveYouTubeUrlHelpers.kt` | Thin backward-compatible wrappers over `YouTubeLinkParser`. |

### Modified

`live/ui/LiveRoomScreen.kt` (state router + session wiring + chat retry + duration badge),
`live/ui/LiveFeedScreen.kt` (opens/closes the room session, host detection),
`live/ui/LiveSetupScreen.kt` (creates persisted rooms for the authenticated user),
`live/viewmodel/LiveViewModel.kt` (identity, session proxies, host commands, follow),
`live/data/DemoLiveRepository.kt` (demo rooms flagged, real rooms cacheable, no Firestore writes),
`live/model/LiveModels.kt` (additive fields only),
`ui/DashboardScreen.kt` + `ui/MainActivity.kt` (share-link intent → room screen),
`AndroidManifest.xml` (App Links + custom scheme for Live),
`firestore.rules` (membership/playback/playlist/chat rules, host-takeover hole closed).

### Removed

`online/FirestoreLiveRepository.kt` — legacy unversioned Live schema (no revision/commandId, no
membership). It was replaced by `FirestoreLiveRoomBackend`; keeping both would have allowed
concurrent writers with different schemas.

---

## 2. Firestore schema

```
live_rooms/{roomId}                                  room + host profile snapshot + isLive flag
live_rooms/{roomId}/members/{uid}                    presence & membership (document id == uid)
live_rooms/{roomId}/watch_party/state                shared playback state (single document)
live_rooms/{roomId}/watch_party_playlist/{videoId}   playlist rows (one document per video)
live_rooms/{roomId}/messages/{clientMessageId}       persistent chat (idempotent document id)
```

- **Playback state is one document.** A play/pause/seek/position correction updates only
  `watch_party/state`, never the playlist, so a queue of 200 items is not rewritten per command.
- **Playback writes are revision-guarded**: `applyPlaybackCommand` runs a transaction, compares the
  persisted `revision` with the caller's `expectedRevision`, and rejects mismatches as a conflict.
- **Chat document id is the client message id**, so a retry after a timeout cannot duplicate a
  message, and ordering/pagination uses `timestampMillis` (a numeric field) with the server
  timestamp stored alongside for auditability.
- **Membership is uid-keyed**, so reconnecting cannot create duplicate presence rows.
- All server-side timestamps use `FieldValue.serverTimestamp()` (`createdAt`, `updatedAt`,
  `timestamp`, `joinedAt`, `lastSeenAt`).

### Security rules (`firestore.rules`)

| Path | Read | Write |
| --- | --- | --- |
| `live_rooms/{id}` | any authenticated user | create: only with `hostUserId == creatorId == auth.uid` and `isLive == true`; update/delete: persisted host only, `hostUserId`/`creatorId` immutable |
| `.../members/{uid}` | room members + host | create/update: `uid == auth.uid` only, `role` must be `VIEWER` unless the writer is the persisted host, room must be live; delete: self or host |
| `.../watch_party/state` | members + host | host only, live room only, `hostUserId == auth.uid`, `revision` int > 0 |
| `.../watch_party_playlist/{videoId}` | members + host | create/delete host only, `addedByUserId == auth.uid`, 11-char `videoId`; updates denied |
| `.../messages/{clientMessageId}` | members only | create: member only, `senderId == auth.uid`, `roomId` matches, `clientMessageId == messageId`, 1..500 chars, `timestamp == request.time`; update/delete denied |

The previous rules allowed a room update when **either** the old or new document claimed
`hostUserId` (takeover hole) and never validated chat senders against membership. Both are fixed. No
existing rule was weakened; `users`, `public_profiles`, `follows`, `blocks`, `conversations`, etc.
are untouched.

Deploy with:

```bash
firebase deploy --only firestore:rules
```

---

## 3. YouTube player

- The player is created through the **official IFrame Player API** (`https://www.youtube.com/iframe_api`)
  inside a `WebView`, with `onYouTubeIframeAPIReady` defined as the API requires.
- Player variables: `playsinline=1` (no forced fullscreen), `controls=1` (YouTube's own controls,
  branding and ads are untouched), `enablejsapi=1`, `rel=0`, `autohide=2`, and an `origin` that
  matches the WebView base URL (`https://workdaily.app`) so `postMessage` control is authorized.
- `videoId` is passed to the `YT.Player` constructor, never as a player variable.
- State is driven by `onReady` / `onStateChange` / `onError` plus a 1 s local position ticker. The
  position ticker never writes to Firestore.
- Error codes are mapped to human messages (invalid id, removed/private, embedding disabled,
  WebView playback failure). **There is no sample-video fallback**: an unusable video shows an error
  with a Retry and a Playlist action, and the room stays usable.
- Navigation is restricted to the embed surface; `Watch on YouTube`/channel taps are handed to the
  system browser via an `ACTION_VIEW` intent instead of replacing the player.
- The WebView is paused with the lifecycle (`WebView.onPause()` + `wpPause()`), the host publishes a
  pause on `ON_STOP`, and the WebView is destroyed with the composable (`stopLoading`,
  `removeJavascriptInterface`, `destroy`).
- Docs checked: <https://developers.google.com/youtube/iframe_api_reference> (requirements/minimum
  200×200 viewport, `origin` recommendation, `onYouTubeIframeAPIReady` contract, Android WebView
  Media Integrity section).

Android WebView note: playback in a WebView participates in the **Android WebView Media Integrity
API**; Google Play services attest the embedding app. Sideloaded/unsigned builds may still be
rejected by YouTube depending on the device/app attestation state. This cannot be verified without a
device.

---

## 4. Playback synchronization

- Only the persisted host (`live_rooms/{id}.hostUserId`) can issue playback commands; the client
  checks this before writing and the rules enforce it server-side.
- Each command produces `revision + 1`, a fresh `commandId`, `updatedByUserId` and `updatedAtMillis`.
- Participants apply a state only when `revision` is newer than the last applied revision, so
  out-of-order/stale snapshots are ignored.
- A host ignores the echo of its own `commandId`, and callbacks caused by applying a remote revision
  are suppressed for 1.5 s to avoid a publish feedback loop.
- Host publishes only real state transitions (`PLAYING → PAUSED`, etc.), not repeated `PLAYING`
  events after buffering; position corrections are bounded (`POSITION_CORRECTION_INTERVAL_MILLIS`)
  and only when drift exceeds the tolerance.
- Late joiners and reconnects load `watch_party/state` and seek to the projected position
  (`position + elapsed` while playing).
- **Mute is local for participants**; the host's shared mute flag is published and applied, while a
  participant's own mute toggle only calls `mute()/unMute()` on their WebView.

---

## 5. Presence, viewers, chat

- Membership is written on session start, refreshed by a heartbeat while the screen is visible, and
  deleted on `stop()`/room exit. A `closed` flag prevents an in-flight heartbeat from re-creating
  the document after leaving.
- **Viewer-count rule:** `viewerCount` = unique non-host members whose `lastSeenAtMillis` is within
  `PresencePolicy.ACTIVE_WINDOW_MILLIS` (75 s), query limit 200. The host is excluded from the
  viewer count and listed separately with a HOST badge. There are no fabricated counts.
- Chat requires membership (client + rules), stamps `senderId` from the authenticated account and
  the display fields from the persisted membership document, so a spoofed name in the payload is
  ignored. Optimistic messages show `Sending…` / `Not sent — tap to retry`; retry reuses the same
  `clientMessageId`.
- Chat history is bounded (latest N) with an explicit “Load earlier messages” cursor, and the
  listener/snapshot registrations are removed when the session stops.

---

## 6. Share links

- Share text: `<Host> is hosting "<title>" (Live|Watch Party) on WorkDaily. Join here: https://workdaily.app/live/{roomId}`
- Manifest intent filters: `https://workdaily.app/live/*` (`pathPrefix=/live/`) and
  `workdaily://live/{roomId}`.
- `MainActivity` resolves the room id, `DashboardScreen` navigates to `live_room/{roomId}`, and
  `LiveFeedScreen` opens the session, which validates that the room exists and is live. Ended or
  missing rooms render an explicit state screen. Because the app requires sign-in before the
  dashboard, opening a link is auth-gated.
- App Links verification is **not** enabled (`autoVerify="false"`): enabling it requires a
  `/.well-known/assetlinks.json` on `workdaily.app` with the release signing SHA-256. Until that
  exists, Android shows a chooser for https links; the `workdaily://` scheme always resolves.

---

## 7. Required configuration

1. `android-offline-attendance-app/app/google-services.json` (Firebase project) — **not present in
   this repository** and intentionally not committed.
2. Firebase Auth (the existing sign-in flow) and Cloud Firestore enabled; `firestore.rules` deployed.
3. Optional Firestore indexes: none required by the queries used here (the “already live?” check is a
   single equality filter, playlist/members/messages are single-collection reads). If you later add
   compound filters (e.g. `isLive == true` + `category`), create the index in the console.
4. `workdaily.app` hosting for share links, plus `/.well-known/assetlinks.json` if App Links
   verification is desired.

No API keys were added anywhere; YouTube oEmbed and the IFrame API are key-less.

---

## 8. Tests

Executed locally with the JVM (`kotlinc` 2.4.21 + a minimal JUnit-compatible runner), because this
sandbox has no Android SDK and cannot reach `dl.google.com`/Maven Central.

| Suite | Scope | Result |
| --- | --- | --- |
| `YouTubeLinkParserTest` | 12 tests: URL/ID parsing, host allowlist, malformed input, player vars, thumbnails | 12/12 pass |
| `WatchPartySyncPolicyTest` | 18 tests: revisions, stale/out-of-order, own-echo, projection, drift, host auth, command application | 18/18 pass |
| `WatchPartyPlaylistLogicTest` | 14 tests: add/duplicate/capacity, remove active/last/non-active, select, next, reconcile, turn off/on | 14/14 pass |
| `LiveShareLinkTest` | 8 tests: round-trip, scheme fallback, query/trailing slash, invalid ids, share text | 8/8 pass |
| `YouTubeOEmbedTest` | 9 tests: endpoint encoding, JSON parsing/escapes, malformed bodies, https-only thumbnails, title bounds, provider failure paths | 9/9 pass |
| `LiveRoomBackendTest` | 20 tests against the in-memory backend: host identity, auth/duplicate creation, membership dedupe, viewer counting, host-only playback, revision conflicts, playlist ops, chat identity/idempotency/limits/pagination, moderation, ended-room behavior | 20/20 pass |
| `LiveRoomSessionTest` | 11 tests: join/presence/heartbeat, participant mirroring + rejection, chat persistence, unauthenticated behavior, end/leave, playlist host-only + metadata titles | 11/11 pass |
| `LiveDurationFormatTest` | 4 tests: duration formatting/derivation | 4/4 pass |
| `WorkDailyLiveTest` (pre-existing) | 18 tests: gifts, wallet, friends, blocks, chat events, follow, YouTube parsing, playlist, revision, turn-off — the existing Live modes regression suite | 18/18 pass |

Totals: **114 tests, 114 passing** (96 new + 18 pre-existing). The suites were executed repeatedly
(including eight consecutive runs of the session suite) to confirm they are not flaky; that exercise
caught and fixed two real races: duplicate playlist rows from double-appending, and a heartbeat
re-creating a presence document after leaving the room.

Not verified here, and why:

- `./gradlew assembleDebug` / `testDebugUnitTest`: no Android SDK in this environment and no network
  access to Google Maven/Maven Central, so the Android/Compose/Firebase modules could not be compiled.
  Only the dependency-free core is compiled and executed.
- Compose UI rendering, WebView playback, device attestation, real Firestore rules evaluation and
  two-device synchronization require a device/emulator and a Firebase project.

---

## 9. Known limitations

- Playlist rows get real titles/thumbnails only when `youtube.com/oembed` answers; otherwise the row
  shows the derived `YouTube Video (<id>)` title with the real thumbnail URL. Nothing is invented.
- Autoplay of unmuted video is subject to platform autoplay policies; a WebView setting allows
  playback without a gesture, but the OS/YouTube may still require one tap. The state converges on
  the next host transition or DRIFT correction.
- Participants tapping inside YouTube's own controls can momentarily diverge; the next revision or
  the drift correction (>2.5 s) realigns them.
- Gift transactions, notifications and moderation keep using their existing repositories; the Watch
  Party only reuses them. Nothing about gifting was re-implemented.
- Demo (`isDemoRoom`) rooms remain local-only; they never write playback state to Firestore.
