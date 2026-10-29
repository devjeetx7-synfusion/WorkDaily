'use strict';

const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret, defineString } = require('firebase-functions/params');
const { createTokenService, TokenError } = require('./token-service');
const { createLiveMediaService } = require('./live-media-service');
const { RoomServiceClient } = require('livekit-server-sdk');

initializeApp();
const apiKey = defineSecret('LIVEKIT_API_KEY');
const apiSecret = defineSecret('LIVEKIT_API_SECRET');
const serverUrl = defineString('LIVEKIT_URL');

const issueToken = createTokenService({
  loadRoom: async (id) => {
    const doc = await getFirestore().collection('live_rooms').doc(id).get();
    return doc.exists ? doc.data() : null;
  },
  isReservationOwned: async (uid, roomId) => {
    const lock = await getFirestore().collection('live_media_host_locks').doc(uid).get();
    return lock.exists && lock.data().roomId === roomId;
  },
  isBlocked: async (uid, host) => {
    const blocks = getFirestore().collection('blocks');
    const docs = await getFirestore().getAll(blocks.doc(`${uid}_${host}`), blocks.doc(`${host}_${uid}`));
    return docs.some((doc) => doc.exists);
  },
  config: () => ({ url: serverUrl.value(), apiKey: apiKey.value(), apiSecret: apiSecret.value() }),
});

// Callable protocol verifies Firebase ID tokens before supplying request.auth. Never accept a
// client-supplied role or uid. Do not log requests, tokens, SDK errors, or secret values.
exports.liveKitToken = onCall({ secrets: [apiKey, apiSecret], timeoutSeconds: 15 }, async (request) => {
  try {
    return await issueToken(request.auth?.uid, request.data);
  } catch (error) {
    if (error instanceof TokenError) throw new HttpsError(error.code, error.message);
    throw new HttpsError('internal', 'Unable to join the live stream. Please try again.');
  }
});


// Reservation and lifecycle endpoints are not yet used by Android. Secrets remain server-side.
const media = createLiveMediaService({
  db: getFirestore(),
  liveKit: () => {
    const url = new URL(serverUrl.value());
    if (url.protocol !== 'wss:' || url.username || url.password) throw new Error('Invalid configuration');
    url.protocol = 'https:';
    return new RoomServiceClient(url.toString(), apiKey.value(), apiSecret.value());
  },
});
function mediaCallable(handler, secrets = []) {
  return onCall({ secrets, timeoutSeconds: 30 }, async request => {
    try { return await handler(request); }
    catch (error) {
      if (error instanceof TokenError) throw new HttpsError(error.code, error.message);
      throw new HttpsError('internal', 'Unable to manage the live stream. Please try again.');
    }
  });
}
exports.reserveLiveMediaRoom = mediaCallable(request => media.reserve(request.auth?.uid, request.data, request.auth?.token?.name));
exports.activateLiveMediaRoom = mediaCallable(request => media.activate(request.auth?.uid, request.data?.roomId), [apiKey, apiSecret]);
exports.endLiveMediaRoom = mediaCallable(request => media.end(request.auth?.uid, request.data?.roomId), [apiKey, apiSecret]);
