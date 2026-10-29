'use strict';

const { AccessToken, TrackSource } = require('livekit-server-sdk');

const TOKEN_TTL_SECONDS = 300;
// Firestore generated IDs are 20 alphanumerics; permit other safe single-segment IDs for migrations.
const ROOM_ID = /^[A-Za-z0-9_-]{1,128}$/;

class TokenError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}

/** Dependencies are injected so policy and token claims can be tested without Firebase. */
function createTokenService({ loadRoom, isBlocked, config }) {
  return async function issueToken(uid, data) {
    if (typeof uid !== 'string' || !uid) throw new TokenError('unauthenticated', 'Sign in to join a live room.');
    const roomId = data && data.roomId;
    if (typeof roomId !== 'string' || !ROOM_ID.test(roomId)) {
      throw new TokenError('invalid-argument', 'Invalid room ID.');
    }
    const room = await loadRoom(roomId);
    if (!room || room.isLive !== true || room.endedAtMillis != null ||
        typeof room.hostUserId !== 'string' || !room.hostUserId) {
      throw new TokenError('failed-precondition', 'This live room is unavailable or has ended.');
    }
    const host = room.hostUserId === uid;
    // Existing membership can be self-created and is NOT a private-room authorization grant.
    if (!host && (room.isPublic !== true || await isBlocked(uid, room.hostUserId))) {
      throw new TokenError('permission-denied', 'You cannot join this live room.');
    }
    const { url, apiKey, apiSecret } = config();
    let parsed;
    try { parsed = new URL(url); } catch (_) { /* fail closed below */ }
    if (!parsed || parsed.protocol !== 'wss:' || parsed.username || parsed.password ||
        !apiKey || !apiSecret) {
      throw new TokenError('failed-precondition', 'Live streaming is not configured.');
    }
    const roomName = `workdaily-${roomId}`;
    const token = new AccessToken(apiKey, apiSecret, { identity: uid, ttl: TOKEN_TTL_SECONDS });
    token.addGrant({ roomJoin: true, room: roomName, canPublish: host,
      canSubscribe: true, canPublishData: false, canUpdateOwnMetadata: false,
      ...(host ? { canPublishSources: [TrackSource.CAMERA, TrackSource.MICROPHONE] } : {}) });
    return { url, token: await token.toJwt(), roomName, expiresInSeconds: TOKEN_TTL_SECONDS };
  };
}
module.exports = { createTokenService, TokenError, TOKEN_TTL_SECONDS };
