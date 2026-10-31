import test from 'node:test';
import assert from 'node:assert/strict';
import { handleTokenRequest, firestoreFieldsToData } from '../src/index.js';

const mockEnv = {
  LIVEKIT_URL: 'wss://livekit.workdaily.example.com',
  LIVEKIT_API_KEY: 'test-api-key',
  LIVEKIT_API_SECRET: '0123456789abcdef0123456789abcdef0123456789abcdef',
  FIREBASE_PROJECT_ID: 'test-firebase-project',
};

test('firestoreFieldsToData converts REST format correctly', () => {
  const fields = {
    title: { stringValue: 'Site Meeting' },
    isLive: { booleanValue: true },
    viewerCount: { integerValue: '42' },
    tags: { arrayValue: { values: [{ stringValue: 'construction' }, { stringValue: 'safety' }] } },
  };
  const parsed = firestoreFieldsToData(fields);
  assert.equal(parsed.title, 'Site Meeting');
  assert.equal(parsed.isLive, true);
  assert.equal(parsed.viewerCount, 42);
  assert.deepEqual(parsed.tags, ['construction', 'safety']);
});

test('handleTokenRequest rejects invalid room ID', async () => {
  const res = await handleTokenRequest({
    uid: 'user1',
    roomId: 'invalid room!@#$',
    idToken: 'token',
    env: mockEnv,
  });
  assert.equal(res.status, 400);
});

test('handleTokenRequest generates host publishing token for valid LIVE room', async () => {
  const room = {
    id: 'room1',
    hostUserId: 'host123',
    mediaManaged: true,
    type: 'VIDEO',
    isLive: true,
    statusText: 'LIVE',
    endedAtMillis: null,
  };

  const res = await handleTokenRequest({
    uid: 'host123',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => room,
  });

  assert.equal(res.status, 200);
  assert.equal(res.body.url, mockEnv.LIVEKIT_URL);
  assert.equal(res.body.roomName, 'workdaily-room1');
  assert.ok(res.body.token);
});

test('handleTokenRequest generates viewer subscribe-only token for joined public room', async () => {
  const room = {
    id: 'room1',
    hostUserId: 'host123',
    mediaManaged: true,
    type: 'VIDEO',
    isLive: true,
    statusText: 'LIVE',
    isPublic: true,
    endedAtMillis: null,
  };

  const res = await handleTokenRequest({
    uid: 'viewer456',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => room,
    fetchMember: async () => ({ uid: 'viewer456' }),
    checkBlocked: async () => false,
  });

  assert.equal(res.status, 200);
  assert.equal(res.body.url, mockEnv.LIVEKIT_URL);
  assert.equal(res.body.roomName, 'workdaily-room1');
  assert.ok(res.body.token);
});

test('handleTokenRequest rejects viewer if not member of the room', async () => {
  const room = {
    id: 'room1',
    hostUserId: 'host123',
    mediaManaged: true,
    type: 'VIDEO',
    isLive: true,
    statusText: 'LIVE',
    isPublic: true,
    endedAtMillis: null,
  };

  const res = await handleTokenRequest({
    uid: 'viewer456',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => room,
    fetchMember: async () => null,
    checkBlocked: async () => false,
  });

  assert.equal(res.status, 403);
  assert.match(res.body.error, /Membership required/);
});

test('handleTokenRequest rejects viewer if room is private', async () => {
  const room = {
    id: 'room1',
    hostUserId: 'host123',
    mediaManaged: true,
    type: 'VIDEO',
    isLive: true,
    statusText: 'LIVE',
    isPublic: false,
    endedAtMillis: null,
  };

  const res = await handleTokenRequest({
    uid: 'viewer456',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => room,
    fetchMember: async () => ({ uid: 'viewer456' }),
    checkBlocked: async () => false,
  });

  assert.equal(res.status, 403);
  assert.match(res.body.error, /private/);
});

test('handleTokenRequest rejects viewer if blocked', async () => {
  const room = {
    id: 'room1',
    hostUserId: 'host123',
    mediaManaged: true,
    type: 'VIDEO',
    isLive: true,
    statusText: 'LIVE',
    isPublic: true,
    endedAtMillis: null,
  };

  const res = await handleTokenRequest({
    uid: 'viewer456',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => room,
    fetchMember: async () => ({ uid: 'viewer456' }),
    checkBlocked: async () => true,
  });

  assert.equal(res.status, 403);
});

test('handleTokenRequest allows host for PREPARING room with valid lock', async () => {
  const room = {
    id: 'room1',
    hostUserId: 'host123',
    mediaManaged: true,
    type: 'VIDEO',
    isLive: false,
    statusText: 'PREPARING',
    reservationExpiresAtMillis: 2000,
    endedAtMillis: null,
  };

  const res = await handleTokenRequest({
    uid: 'host123',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => room,
    fetchLock: async () => ({ roomId: 'room1' }),
    clock: () => 1000,
  });

  assert.equal(res.status, 200);
  assert.ok(res.body.token);
});

test('never leaks API secret in response or error', async () => {
  const res = await handleTokenRequest({
    uid: 'host123',
    roomId: 'room1',
    idToken: 'mockToken',
    env: mockEnv,
    fetchRoom: async () => { throw new Error('database connection down'); },
  });
  const serialized = JSON.stringify(res);
  assert.equal(serialized.includes(mockEnv.LIVEKIT_API_SECRET), false);
});
