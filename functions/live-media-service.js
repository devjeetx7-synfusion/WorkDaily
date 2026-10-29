'use strict';

const { TokenError } = require('./token-service');
const { ParticipantInfo_State } = require('livekit-server-sdk');
const HOST_ABSENCE_GRACE_MS = 3 * 60 * 1000;
// Do not treat a long monitoring outage as evidence of continuous absence.
const MAX_OBSERVATION_GAP_MS = 24 * 60 * 60 * 1000;
const { RESERVATION_MS, validateRoomId, validateReservation, validatePublished } = require('./live-media-policy');

/** Server-side room transitions. Caller identity must come from the callable's verified auth. */
function createLiveMediaService({ db, liveKit, clock = Date.now }) {
  const rooms = db.collection('live_rooms');
  const locks = db.collection('live_media_host_locks');
  function requireUid(uid) {
    if (!uid) throw new TokenError('unauthenticated', 'Sign in first.');
    // Firebase UIDs cannot be used as Firestore document paths if they contain a slash.
    if (typeof uid !== 'string' || uid.includes('/')) throw new TokenError('permission-denied', 'Invalid account.');
  }
  async function reserve(uid, data, displayName) {
    requireUid(uid);
    const type = data?.type;
    if (type !== 'VIDEO' && type !== 'AUDIO') throw new TokenError('invalid-argument', 'Invalid broadcast type.');
    const title = typeof data.title === 'string' ? data.title.trim() : '';
    if (!title || title.length > 120 || typeof data.isPublic !== 'boolean') throw new TokenError('invalid-argument', 'Invalid room settings.');
    const name = typeof displayName === 'string' && displayName.trim() ? displayName.trim().slice(0, 100) : 'WorkDaily Host';
    const lockRef = locks.doc(uid);
    const freshRef = rooms.doc();
    return db.runTransaction(async tx => {
      const lock = await tx.get(lockRef);
      const oldId = lock.exists ? lock.data().roomId : null;
      let oldRef, oldSnapshot;
      if (typeof oldId === 'string' && ROOM_ID_SAFE(oldId)) {
        oldRef = rooms.doc(oldId);
        oldSnapshot = await tx.get(oldRef);
      }
      const now = clock();
      const old = oldSnapshot?.exists ? oldSnapshot.data() : null;
      if (old && old.hostUserId === uid && old.mediaManaged === true && old.endedAtMillis == null) {
        if (old.isLive === true || (old.statusText === 'PREPARING' && old.reservationExpiresAtMillis > now)) {
          return { roomId: oldId, status: old.statusText };
        }
        if (old.statusText === 'PREPARING') tx.update(oldRef, { statusText: 'FAILED', isLive: false, endedAtMillis: now, cleanupPending: true });
      }
      tx.set(freshRef, {
        id: freshRef.id, hostUserId: uid, creatorId: uid, creatorName: name, hostName: name,
        title, type, category: 'Site Safety', isPublic: false, requestedIsPublic: data.isPublic,
        isChatEnabled: data.isChatEnabled !== false, showGifts: data.showGifts !== false,
        allowGuestRequests: false, isLive: false, mediaManaged: true, statusText: 'PREPARING',
        reservationExpiresAtMillis: now + RESERVATION_MS, createdAtMillis: now, endedAtMillis: null,
      });
      tx.set(lockRef, { roomId: freshRef.id });
      return { roomId: freshRef.id, status: 'PREPARING' };
    });
  }
  async function activate(uid, id) {
    requireUid(uid); validateRoomId(id);
    const ref = rooms.doc(id);
    const snapshot = await ref.get();
    const room = snapshot.exists ? snapshot.data() : null;
    if (room?.mediaManaged === true && room.hostUserId === uid && room.isLive === true &&
        room.statusText === 'LIVE' && room.endedAtMillis == null) return { roomId: id, status: 'LIVE' };
    validateReservation(uid, room, clock());
    // The backend, not the client, checks LiveKit's connected participant and publications.
    const participant = await liveKit().getParticipant(`workdaily-${id}`, uid);
    validatePublished(room, participant);
    return db.runTransaction(async tx => {
      const fresh = await tx.get(ref);
      const lock = await tx.get(locks.doc(uid));
      const current = fresh.exists ? fresh.data() : null;
      validateReservation(uid, current, clock());
      if (!lock.exists || lock.data().roomId !== id) throw new TokenError('failed-precondition', 'Reservation not found.');
      // Make type/host immutable across the external check and the transition.
      if (current.type !== room.type) throw new TokenError('failed-precondition', 'Room changed.');
      tx.update(ref, { isLive: true, isPublic: current.requestedIsPublic === true, statusText: 'LIVE', startTimeMillis: clock() });
      return { roomId: id, status: 'LIVE' };
    });
  }
  async function end(uid, id) {
    requireUid(uid); validateRoomId(id);
    const ref = rooms.doc(id);
    const lockRef = locks.doc(uid);
    await db.runTransaction(async tx => {
      const snapshot = await tx.get(ref);
      const lock = await tx.get(lockRef);
      const room = snapshot.exists ? snapshot.data() : null;
      if (!room || room.mediaManaged !== true || room.hostUserId !== uid) throw new TokenError('permission-denied', 'You cannot manage this room.');
      if (room.endedAtMillis == null) tx.update(ref, { isLive: false, statusText: 'ENDED', endedAtMillis: clock(), cleanupPending: true });
      else if (room.cleanupPending !== true) tx.update(ref, { cleanupPending: true });
      if (lock.exists && lock.data().roomId === id) tx.delete(lockRef);
    });
    // Retryable: a failure to close media is reported, while Firestore is already not live.
    try { await liveKit().deleteRoom(`workdaily-${id}`); }
    catch (error) {
      // A room that is already absent is already cleaned up. Other errors remain retryable.
      if (error?.status !== 404 && error?.code !== 'not_found') throw error;
    }
    await db.runTransaction(async tx => {
      const fresh = await tx.get(ref);
      if (fresh.exists && fresh.data().mediaManaged === true && fresh.data().endedAtMillis != null && fresh.data().cleanupPending === true) {
        tx.update(ref, { cleanupPending: false });
      }
    });
    return { roomId: id, status: 'ENDED' };
  }

  // Reconciliation never changes a room based on a LiveKit transport/5xx error. A missing
  // host must be observed twice, at least HOST_ABSENCE_GRACE_MS apart, before ending a live.
  async function reconcile(id) {
    validateRoomId(id);
    const ref = rooms.doc(id);
    const snapshot = await ref.get();
    if (!snapshot.exists) return 'missing';
    const room = snapshot.data();
    if (room.mediaManaged !== true || typeof room.hostUserId !== 'string' || !room.hostUserId || room.hostUserId.includes('/')) return 'ignored';
    const now = clock();
    if (room.statusText === 'PREPARING' && room.isLive === false && room.endedAtMillis == null &&
        Number.isSafeInteger(room.reservationExpiresAtMillis) && room.reservationExpiresAtMillis <= now) {
      await db.runTransaction(async tx => {
        const fresh = await tx.get(ref);
        const lock = await tx.get(locks.doc(room.hostUserId));
        const current = fresh.exists ? fresh.data() : null;
        if (!current || current.mediaManaged !== true || current.hostUserId !== room.hostUserId ||
            current.statusText !== 'PREPARING' || current.isLive !== false || current.endedAtMillis != null ||
            !Number.isSafeInteger(current.reservationExpiresAtMillis) || current.reservationExpiresAtMillis > clock()) return;
        tx.update(ref, { statusText: 'FAILED', endedAtMillis: clock(), cleanupPending: true });
        if (lock.exists && lock.data().roomId === id) tx.delete(locks.doc(room.hostUserId));
      });
      return 'expired';
    }
    // Rooms ended before the cleanup marker was introduced must also be retried.
    if (room.endedAtMillis != null && room.cleanupPending == null) {
      await db.runTransaction(async tx => {
        const fresh = await tx.get(ref);
        if (fresh.exists && fresh.data().mediaManaged === true && fresh.data().endedAtMillis != null && fresh.data().cleanupPending == null) {
          tx.update(ref, { cleanupPending: true });
        }
      });
      return 'marked';
    }
    if (room.endedAtMillis != null && room.cleanupPending === true) {
      try { await liveKit().deleteRoom(`workdaily-${id}`); }
      catch (error) { if (error?.status !== 404 && error?.code !== 'not_found') throw error; }
      await db.runTransaction(async tx => {
        const fresh = await tx.get(ref);
        if (fresh.exists && fresh.data().mediaManaged === true && fresh.data().endedAtMillis != null && fresh.data().cleanupPending === true) {
          tx.update(ref, { cleanupPending: false });
        }
      });
      return 'cleaned';
    }
    if (room.isLive !== true || room.statusText !== 'LIVE' || room.endedAtMillis != null) return 'ignored';
    let present;
    try {
      const participant = await liveKit().getParticipant(`workdaily-${id}`, room.hostUserId);
      present = !!participant && participant.identity === room.hostUserId && participant.state !== ParticipantInfo_State.DISCONNECTED;
    } catch (error) {
      if (error?.status !== 404 && error?.code !== 'not_found') throw error;
      present = false;
    }
    return db.runTransaction(async tx => {
      const fresh = await tx.get(ref);
      const lock = await tx.get(locks.doc(room.hostUserId));
      const current = fresh.exists ? fresh.data() : null;
      if (!current || current.mediaManaged !== true || current.hostUserId !== room.hostUserId ||
          current.isLive !== true || current.statusText !== 'LIVE' || current.endedAtMillis != null ||
          current.startTimeMillis !== room.startTimeMillis) return 'changed';
      if (present) {
        if (current.hostMissingSinceMillis != null || current.hostMissingLastCheckedMillis != null) {
          tx.update(ref, { hostMissingSinceMillis: null, hostMissingLastCheckedMillis: null });
        }
        return 'healthy';
      }
      if (!Number.isSafeInteger(current.hostMissingSinceMillis) || current.hostMissingSinceMillis > now ||
          !Number.isSafeInteger(current.hostMissingLastCheckedMillis) || current.hostMissingLastCheckedMillis > now ||
          now - current.hostMissingLastCheckedMillis > MAX_OBSERVATION_GAP_MS) {
        tx.update(ref, { hostMissingSinceMillis: now, hostMissingLastCheckedMillis: now });
        return 'grace';
      }
      if (now - current.hostMissingSinceMillis < HOST_ABSENCE_GRACE_MS) {
        tx.update(ref, { hostMissingLastCheckedMillis: now });
        return 'grace';
      }
      tx.update(ref, { isLive: false, statusText: 'ENDED', endedAtMillis: now, cleanupPending: true });
      if (lock.exists && lock.data().roomId === id) tx.delete(locks.doc(room.hostUserId));
      return 'ended';
    });
  }
  return { reserve, activate, end, reconcile };
}
function ROOM_ID_SAFE(value) { return /^[A-Za-z0-9_-]{1,128}$/.test(value); }
module.exports = { createLiveMediaService, HOST_ABSENCE_GRACE_MS, MAX_OBSERVATION_GAP_MS };
