'use strict';
const { TrackSource, ParticipantInfo_State } = require('livekit-server-sdk');
const { TokenError } = require('./token-service');

const RESERVATION_MS = 10 * 60 * 1000;
const ROOM_ID = /^[A-Za-z0-9_-]{1,128}$/;
function validateRoomId(id) {
  if (typeof id !== 'string' || !ROOM_ID.test(id)) throw new TokenError('invalid-argument', 'Invalid room ID.');
}
function validateReservation(uid, room, now) {
  if (!uid) throw new TokenError('unauthenticated', 'Sign in first.');
  if (!room || room.mediaManaged !== true || room.hostUserId !== uid) {
    throw new TokenError('permission-denied', 'You cannot manage this room.');
  }
  if (room.isLive !== false || room.statusText !== 'PREPARING' || room.endedAtMillis != null ||
      !Number.isSafeInteger(room.reservationExpiresAtMillis) || room.reservationExpiresAtMillis <= now) {
    throw new TokenError('failed-precondition', 'The reservation is no longer available.');
  }
}
function validatePublished(room, participant) {
  if (!participant || participant.identity !== room.hostUserId || participant.state !== ParticipantInfo_State.ACTIVE) {
    throw new TokenError('failed-precondition', 'The host is not connected.');
  }
  const tracks = participant.tracks || [];
  const has = (source) => tracks.some((track) => track.source === source && track.muted === false);
  if (!has(TrackSource.MICROPHONE) || (room.type === 'VIDEO' && !has(TrackSource.CAMERA))) {
    throw new TokenError('failed-precondition', 'Required media has not started.');
  }
}
module.exports = { RESERVATION_MS, validateRoomId, validateReservation, validatePublished };
