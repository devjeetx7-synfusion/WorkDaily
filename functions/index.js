'use strict';

const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret, defineString } = require('firebase-functions/params');
const { createTokenService, TokenError } = require('./token-service');

initializeApp();
const apiKey = defineSecret('LIVEKIT_API_KEY');
const apiSecret = defineSecret('LIVEKIT_API_SECRET');
const serverUrl = defineString('LIVEKIT_URL');

const issueToken = createTokenService({
  loadRoom: async (id) => {
    const doc = await getFirestore().collection('live_rooms').doc(id).get();
    return doc.exists ? doc.data() : null;
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
