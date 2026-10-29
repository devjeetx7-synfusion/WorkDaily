'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { TrackSource, ParticipantInfo_State } = require('livekit-server-sdk');
const { validateRoomId, validateReservation, validatePublished } = require('../live-media-policy');
const now = 1000;
const room = { hostUserId: 'host', mediaManaged: true, isLive: false, statusText: 'PREPARING', endedAtMillis: null, reservationExpiresAtMillis: 2000, type: 'VIDEO' };
const participant = { identity: 'host', state: ParticipantInfo_State.ACTIVE, tracks: [
  { source: TrackSource.CAMERA, muted: false }, { source: TrackSource.MICROPHONE, muted: false },
] };
function denied(f, code) { assert.throws(f, e => e.code === code); }
test('reservation requires persisted owner, server marker, preparing status and expiry', () => {
  validateReservation('host', room, now);
  denied(() => validateReservation('viewer', room, now), 'permission-denied');
  denied(() => validateReservation('host', { ...room, mediaManaged: false }, now), 'permission-denied');
  denied(() => validateReservation('host', { ...room, reservationExpiresAtMillis: now }, now), 'failed-precondition');
  denied(() => validateReservation('host', { ...room, statusText: 'ENDED' }, now), 'failed-precondition');
  denied(() => validateRoomId('../foo'), 'invalid-argument');
});
test('activation requires livekit active host and required unmuted tracks', () => {
  validatePublished(room, participant);
  denied(() => validatePublished(room, { ...participant, identity: 'viewer' }), 'failed-precondition');
  denied(() => validatePublished(room, { ...participant, state: ParticipantInfo_State.DISCONNECTED }), 'failed-precondition');
  denied(() => validatePublished(room, { ...participant, tracks: [participant.tracks[0]] }), 'failed-precondition');
  denied(() => validatePublished(room, { ...participant, tracks: [{ ...participant.tracks[0], muted: true }, participant.tracks[1]] }), 'failed-precondition');
  validatePublished({ ...room, type: 'AUDIO' }, { ...participant, tracks: [participant.tracks[1]] });
});
