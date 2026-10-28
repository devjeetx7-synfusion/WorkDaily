# WorkDaily Live - Same-Device / Cloned App Testing Diagnostic

## Problem Statement
When testing WorkDaily Live with a cloned app (e.g. Dual Apps, Parallel Space, App Cloner) on a single physical Android device, the second app instance cannot sign in or start a separate test session.

## Confirmed Technical Root Causes

### 1. Google Identity & Firebase Auth Package Attestation
* **Package Name & SHA-1 Mismatch:** Google Sign-In via Android Credential Manager (`GetGoogleIdOption`) passes the calling Android package name and certificate SHA-1 fingerprint to Google Play Services.
* App cloning tools alter the package name (e.g., `com.synfusion.attendance.clone`) or sign the cloned APK with a generic debug certificate.
* Google Cloud Console and Firebase Auth enforce strict package name and SHA-1 registration (`google-services.json`). Calling Google Sign-In from an unregistered package causes a `GetCredentialException` (`DEVELOPER_ERROR` / `12500`).
* Firebase App Check / Play Integrity attestation rejects modified or cloned package signatures.

### 2. Single Active Room Constraint (`isLive == true` per UID)
* In `FirestoreLiveRoomBackend.kt`, `createRoom()` checks whether the authenticated host UID already has an active room:
  ```kotlin
  val existing = firestore.collection("live_rooms")
      .whereEqualTo("hostUserId", uid)
      .limit(20)
      .get()
  ```
* If Instance A and Instance B share the same Firebase account (same UID `X`), Instance A's active room blocks Instance B from starting a second room under the same UID (`"You already have an active live room. End it before starting a new one."`).
* This rule protects production data integrity against orphaned streams and state corruption and must never be bypassed for production users.

### 3. Shared System OAuth Session Routing
* On a single Android OS user profile, Google Play Services routes OAuth callbacks to the main package installation. Signing in on Instance B can overwrite or invalidate the active token on Instance A.

## Recommended Testing Approaches

To test multi-user Live interactions on a single machine or device without disabling production security safeguards:

1. **Android OS Multi-User Profiles / Work Profiles:**
   - Use Android System Settings -> Users & Accounts -> Add User/Profile.
   - Each Android OS user profile has a completely isolated Google Play Services context, package namespace, and Google account login.

2. **Multiple Emulators or Physical Devices:**
   - Run Instance A on Device/Emulator 1 (signed in as User A) and Instance B on Device/Emulator 2 (signed in as User B).

3. **JVM Unit Testing (`InMemoryLiveRoomBackend`):**
   - For automated verification, `InMemoryLiveRoomBackend` simulates multi-user room creation, playback synchronization, chat, and presence entirely in memory without requiring Google Play Services or network connectivity.
