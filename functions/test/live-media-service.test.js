'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { TrackSource, ParticipantInfo_State } = require('livekit-server-sdk');
const { createLiveMediaService } = require('../live-media-service');

// Transaction semantics are simulated sequentially; this does not replace emulator race tests.
function fixture() {
  const data = new Map(); let next = 0; let now = 1_000_000; let deletes = 0; let deleteFailure = false;
  const doc = (collection, id) => ({ id, key: `${collection}/${id}`, async get() { return snap(this); } });
  const snap = ref => ({ exists: data.has(ref.key), data: () => data.get(ref.key) });
  const db = {
    collection: name => ({ doc: id => doc(name, id || `room${++next}`) }),
    runTransaction: async fn => {
      const writes = [];
      const tx = { get: async ref => snap(ref),
        set: (ref, value) => writes.push(() => data.set(ref.key, value)),
        update: (ref, value) => writes.push(() => data.set(ref.key, { ...data.get(ref.key), ...value })),
        delete: ref => writes.push(() => data.delete(ref.key)) };
      const result = await fn(tx); writes.forEach(write => write()); return result;
    },
  };
  let participant = { identity: 'host', state: ParticipantInfo_State.ACTIVE, tracks: [
    { source: TrackSource.CAMERA, muted: false }, { source: TrackSource.MICROPHONE, muted: false },
  ] };
  const service = createLiveMediaService({ db, clock: () => now, liveKit: () => ({
    getParticipant: async () => participant,
    deleteRoom: async () => { deletes++; if (deleteFailure) throw new Error('network unavailable'); if (deletes > 1) throw { status: 404 }; },
  }) });
  return { data, service, setParticipant: p => { participant = p; }, advance: ms => { now += ms; }, deletes: () => deletes, failDeletes: value => { deleteFailure = value; } };
}
const settings = { type: 'VIDEO', title: 'Test broadcast', isPublic: true };
test('reserve is private until verified activation, retryable, and host owned', async () => {
  const f = fixture();
  const first = await f.service.reserve('host', settings, 'Host');
  assert.deepEqual(await f.service.reserve('host', settings, 'Host'), first);
  const room = f.data.get(`live_rooms/${first.roomId}`);
  assert.equal(room.isLive, false); assert.equal(room.isPublic, false); assert.equal(room.statusText, 'PREPARING');
  await assert.rejects(f.service.activate('viewer', first.roomId), e => e.code === 'permission-denied');
  f.setParticipant(null);
  await assert.rejects(f.service.activate('host', first.roomId), e => e.code === 'failed-precondition');
  assert.equal(f.data.get(`live_rooms/${first.roomId}`).isLive, false);
});
test('verified activation and end are idempotent, and 404 media cleanup is safe', async () => {
  const f = fixture();
  const { roomId } = await f.service.reserve('host', settings, 'Host');
  assert.equal((await f.service.activate('host', roomId)).status, 'LIVE');
  assert.equal((await f.service.activate('host', roomId)).status, 'LIVE');
  assert.equal(f.data.get(`live_rooms/${roomId}`).isLive, true);
  assert.equal(f.data.get(`live_rooms/${roomId}`).isPublic, true);
  await assert.rejects(f.service.end('viewer', roomId), e => e.code === 'permission-denied');
  assert.equal((await f.service.end('host', roomId)).status, 'ENDED');
  assert.equal((await f.service.end('host', roomId)).status, 'ENDED');
  assert.equal(f.data.get(`live_rooms/${roomId}`).isLive, false);
  assert.equal(f.deletes(), 2);
});
test('expired preparation fails closed and next reservation ends it before replacement', async () => {
  const f = fixture();
  const old = await f.service.reserve('host', settings, 'Host');
  f.advance(600_001);
  await assert.rejects(f.service.activate('host', old.roomId), e => e.code === 'failed-precondition');
  const fresh = await f.service.reserve('host', settings, 'Host');
  assert.notEqual(old.roomId, fresh.roomId);
  assert.equal(f.data.get(`live_rooms/${old.roomId}`).statusText, 'FAILED');
  assert.equal(f.data.get(`live_rooms/${old.roomId}`).isLive, false);
});

test('failed media cleanup does not restore live status and can be retried', async () => {
  const f = fixture();
  const { roomId } = await f.service.reserve('host', settings, 'Host');
  await f.service.activate('host', roomId);
  f.failDeletes(true);
  await assert.rejects(f.service.end('host', roomId), /network unavailable/);
  assert.equal(f.data.get(`live_rooms/${roomId}`).statusText, 'ENDED');
  assert.equal(f.data.get(`live_rooms/${roomId}`).isLive, false);
  f.failDeletes(false);
  assert.equal((await f.service.end('host', roomId)).status, 'ENDED');
});

test('activation fails closed when the host lock no longer refers to the reservation', async () => {
  const f = fixture();
  const { roomId } = await f.service.reserve('host', settings, 'Host');
  f.data.set('live_media_host_locks/host', { roomId: 'different' });
  await assert.rejects(f.service.activate('host', roomId), e => e.code === 'failed-precondition');
  assert.equal(f.data.get(`live_rooms/${roomId}`).isLive, false);
  await assert.rejects(f.service.end(undefined, roomId), e => e.code === 'unauthenticated');
});
