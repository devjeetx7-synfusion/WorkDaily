'use strict';

const { TokenError } = require('./token-service');
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
        if (old.statusText === 'PREPARING') tx.update(oldRef, { statusText: 'FAILED', isLive: false, endedAtMillis: now });
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
      if (room.endedAtMillis == null) tx.update(ref, { isLive: false, statusText: 'ENDED', endedAtMillis: clock() });
      if (lock.exists && lock.data().roomId === id) tx.delete(lockRef);
    });
    // Retryable: a failure to close media is reported, while Firestore is already not live.
    try { await liveKit().deleteRoom(`workdaily-${id}`); }
    catch (error) {
      // A room that is already absent is already cleaned up. Other errors remain retryable.
      if (error?.status !== 404 && error?.code !== 'not_found') throw error;
    }
    return { roomId: id, status: 'ENDED' };
  }
  return { reserve, activate, end };
}
function ROOM_ID_SAFE(value) { return /^[A-Za-z0-9_-]{1,128}$/.test(value); }
module.exports = { createLiveMediaService };
