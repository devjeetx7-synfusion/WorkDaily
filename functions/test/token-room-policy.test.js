'use strict';

// Exercise the production token policy without installing or contacting LiveKit.
// The SDK is a stub here: JWT signing itself remains covered by token-service.test.js.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

class FakeToken {
  constructor(_key, _secret, options) { this.identity = options.identity; }
  addGrant(grant) { this.grant = grant; }
  async toJwt() { return JSON.stringify({ identity: this.identity, grant: this.grant }); }
}
const testModule = { exports: {} };
vm.runInNewContext(fs.readFileSync(path.join(__dirname, '..', 'token-service.js'), 'utf8'), {
  module: testModule,
  require: name => {
    assert.equal(name, 'livekit-server-sdk');
    return { AccessToken: FakeToken, TrackSource: { CAMERA: 'camera', MICROPHONE: 'microphone' } };
  },
  URL, Date,
});
const { createTokenService } = testModule.exports;
const active = { mediaManaged: true, type: 'VIDEO', statusText: 'LIVE', isLive: true,
  endedAtMillis: null, isPublic: true, hostUserId: 'host' };
const request = { roomId: 'room1' };
function issue(room, { blocked = false, member = true, lock = true, now = 1000 } = {}) {
  return createTokenService({
    loadRoom: async () => room,
    isBlocked: async () => blocked,
    isMember: async () => member,
    isReservationOwned: async () => lock,
    config: () => ({ url: 'wss://example.com', apiKey: 'key', apiSecret: 'test' }),
    clock: () => now,
  });
}
async function denied(promise, code) {
  await assert.rejects(promise, error => error.code === code);
}

test('managed active rooms grant only their persisted host publication rights', async () => {
  const host = JSON.parse((await issue(active)('host', request)).token);
  const viewer = JSON.parse((await issue(active)('viewer', { ...request, isHost: true, hostUserId: 'viewer' })).token);
  assert.equal(host.grant.canPublish, true);
  assert.equal(host.grant.room, 'workdaily-room1');
  assert.equal(viewer.grant.canPublish, false);
  assert.equal(viewer.grant.canSubscribe, true);
  assert.equal(viewer.identity, 'viewer');
});

test('unmanaged, wrong-type, ended and inconsistent rooms fail closed for both roles', async () => {
  for (const room of [
    { ...active, mediaManaged: false }, { ...active, type: 'TEXT' },
    { ...active, type: 'YOUTUBE_WATCH_PARTY' }, { ...active, type: null },
    { ...active, endedAtMillis: 3 }, { ...active, statusText: 'ENDED' },
    { ...active, statusText: 'PREPARING' },
  ]) {
    await denied(issue(room)('host', request), 'failed-precondition');
    await denied(issue(room)('viewer', request), 'failed-precondition');
  }
});

test('private and blocked viewers are denied and audio managed rooms are accepted', async () => {
  await denied(issue({ ...active, isPublic: false })('viewer', request), 'permission-denied');
  await denied(issue(active, { blocked: true })('viewer', request), 'permission-denied');
  await denied(issue(active, { member: false })('viewer', request), 'permission-denied');
  const audio = JSON.parse((await issue({ ...active, type: 'AUDIO' })('host', request)).token);
  assert.equal(audio.grant.canPublish, true);
  assert.deepEqual(audio.grant.canPublishSources, ['microphone']);
});

test('preparing room requires the host, valid expiry and matching trusted lock', async () => {
  const preparing = { ...active, isLive: false, statusText: 'PREPARING', reservationExpiresAtMillis: 2000 };
  assert.equal(JSON.parse((await issue(preparing)('host', request)).token).grant.canPublish, true);
  await denied(issue(preparing)('viewer', request), 'failed-precondition');
  await denied(issue(preparing, { lock: false })('host', request), 'failed-precondition');
  await denied(issue(preparing, { now: 2000 })('host', request), 'failed-precondition');
});

test('missing membership dependency and membership lookup errors fail closed', async () => {
  const base = {
    loadRoom: async () => active,
    isBlocked: async () => false,
    config: () => ({ url: 'wss://example.com', apiKey: 'key', apiSecret: 'test' }),
  };
  await denied(createTokenService(base)('viewer', request), 'permission-denied');
  await assert.rejects(createTokenService({ ...base, isMember: async () => { throw Error('unavailable'); } })('viewer', request));
});
