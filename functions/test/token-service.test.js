'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { TokenVerifier } = require('livekit-server-sdk');
const { createTokenService, TOKEN_TTL_SECONDS } = require('../token-service');

const apiKey = 'test-key';
const apiSecret = 'test-secret-not-for-production-12345';
const active = { hostUserId: 'host-uid', isLive: true, endedAtMillis: null, isPublic: true };
function service(room = active, blocked = false, overrides = {}) {
  return createTokenService({
    loadRoom: async () => room,
    isBlocked: async () => blocked,
    config: () => ({ url: 'wss://live.example.com', apiKey, apiSecret }),
    ...overrides,
  });
}
const request = { roomId: 'room_123' };
async function claims(result) { return new TokenVerifier(apiKey, apiSecret).verify(result.token); }
async function rejectsCode(promise, code) {
  await assert.rejects(promise, (error) => error.code === code);
}

test('host receives a room-scoped publishing grant bound to Firebase identity', async () => {
  const result = await service()('host-uid', { ...request, isHost: false });
  const jwt = await claims(result);
  assert.equal(jwt.sub, 'host-uid');
  assert.equal(jwt.video.room, 'workdaily-room_123');
  assert.equal(jwt.video.roomJoin, true);
  assert.equal(jwt.video.canPublish, true);
  assert.deepEqual(jwt.video.canPublishSources, ['camera', 'microphone']);
  assert.equal(jwt.video.canSubscribe, true);
  assert.equal(jwt.video.canPublishData, false);
  assert.ok(jwt.exp - Math.floor(Date.now() / 1000) <= TOKEN_TTL_SECONDS);
  assert.ok(jwt.exp - Math.floor(Date.now() / 1000) >= TOKEN_TTL_SECONDS - 5);
  assert.equal(result.expiresInSeconds, TOKEN_TTL_SECONDS);
  assert.equal(result.roomName, jwt.video.room);
  assert.equal(result.url, 'wss://live.example.com');
  assert.equal(JSON.stringify(result).includes(apiSecret), false);
});

test('viewer cannot escalate by supplying host flags or identities', async () => {
  const jwt = await claims(await service()('viewer', { ...request, isHost: true, hostUserId: 'viewer' }));
  assert.equal(jwt.sub, 'viewer');
  assert.equal(jwt.video.canPublish, false);
  assert.equal(jwt.video.canSubscribe, true);
  assert.equal(jwt.video.canPublishData, false);
});

test('private and blocked viewers are rejected, but host may join private room', async () => {
  const privateRoom = { ...active, isPublic: false };
  await rejectsCode(service(privateRoom)('viewer', request), 'permission-denied');
  await rejectsCode(service(active, true)('viewer', request), 'permission-denied');
  assert.equal((await claims(await service(privateRoom)('host-uid', request))).video.canPublish, true);
});

test('missing, ended and malformed rooms fail closed', async () => {
  for (const room of [null, { ...active, isLive: false }, { ...active, endedAtMillis: 42 }, { ...active, hostUserId: '' }]) {
    await rejectsCode(service(room)('host-uid', request), 'failed-precondition');
  }
  for (const roomId of ['', '../secret', 'a/b', '.', 'x'.repeat(129), 123, null]) {
    await rejectsCode(service()('host-uid', { roomId }), 'invalid-argument');
  }
});

test('missing authentication and missing or insecure configuration fail closed', async () => {
  await rejectsCode(service()(undefined, request), 'unauthenticated');
  for (const cfg of [{}, { url: 'ws://localhost', apiKey, apiSecret }, { url: 'wss://user:pass@host', apiKey, apiSecret }]) {
    await rejectsCode(service(active, false, { config: () => cfg })('host-uid', request), 'failed-precondition');
  }
});

test('database failures never mint a token', async () => {
  await assert.rejects(service(active, false, { loadRoom: async () => { throw Error('unavailable'); } })('host-uid', request));
  await assert.rejects(service(active, false, { isBlocked: async () => { throw Error('unavailable'); } })('viewer', request));
});
