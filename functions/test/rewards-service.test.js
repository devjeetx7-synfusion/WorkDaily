'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { createRewardsService } = require('../rewards-service');

function createMockFirestore() {
  const store = new Map();

  function getDocRef(path) {
    return {
      path,
      id: path.split('/').pop(),
      collection: (sub) => getColRef(`${path}/${sub}`),
      get: async () => {
        const data = store.get(path);
        return {
          exists: data !== undefined,
          data: () => data
        };
      }
    };
  }

  function getColRef(path) {
    return {
      path,
      doc: (id) => {
        const docId = id || `auto_${Math.random().toString(36).substring(2, 9)}`;
        return getDocRef(`${path}/${docId}`);
      },
      orderBy: () => ({
        limit: () => ({
          get: async () => {
            const prefix = `${path}/`;
            const docs = [];
            for (const [k, v] of store.entries()) {
              if (k.startsWith(prefix) && k.split('/').length === path.split('/').length + 1) {
                docs.push({ id: k.split('/').pop(), data: () => v });
              }
            }
            return { docs, size: docs.length };
          }
        })
      }),
      where: () => ({
        get: async () => ({ docs: [], size: 0 })
      })
    };
  }

  return {
    store,
    collection: getColRef,
    doc: getDocRef,
    runTransaction: async (updateFunction) => {
      const transaction = {
        get: async (docRef) => {
          const data = store.get(docRef.path);
          return {
            exists: data !== undefined,
            data: () => data
          };
        },
        set: (docRef, data, options) => {
          if (options && options.merge && store.has(docRef.path)) {
            const existing = store.get(docRef.path);
            store.set(docRef.path, { ...existing, ...data });
          } else {
            store.set(docRef.path, data);
          }
        },
        update: (docRef, data) => {
          const existing = store.get(docRef.path) || {};
          store.set(docRef.path, { ...existing, ...data });
        }
      };
      return await updateFunction(transaction);
    }
  };
}

test('rewardsGetState initializes user wallet and referral code', async () => {
  const db = createMockFirestore();
  const rewards = createRewardsService({ db });

  const state = await rewards.getState('user123');
  assert.equal(state.walletSummary.availableCoins, 0);
  assert.equal(state.spinTokens.availableSpins, 1);
  assert.ok(state.referralStats.referralCode.startsWith('WD'));
});

test('rewardsDailyCheckIn prevents duplicate check-in on same day', async () => {
  const db = createMockFirestore();
  const rewards = createRewardsService({ db });

  const res1 = await rewards.dailyCheckIn('user123');
  assert.equal(res1.success, true);
  assert.equal(res1.availableCoins, 5);
  assert.equal(res1.availableSpins, 2);

  await assert.rejects(
    async () => { await rewards.dailyCheckIn('user123'); },
    (err) => err.code === 'already-exists'
  );
});

test('rewardsSpinWheel enforces spin token consumption and idempotency', async () => {
  const db = createMockFirestore();
  // Fixed random supplier selecting sector index 0 (Safety Helmet, 2 coins)
  const rewards = createRewardsService({ db, randomSupplier: () => 0.0 });

  await rewards.getState('user123'); // availableSpins = 1

  const spinResult1 = await rewards.spinWheel('user123', 'req_abc');
  assert.equal(spinResult1.sectorIndex, 0);
  assert.equal(spinResult1.availableCoins, 2);
  assert.equal(spinResult1.availableSpins, 0);

  // Retry with same requestId returns identical result without double consuming
  const spinResult2 = await rewards.spinWheel('user123', 'req_abc');
  assert.equal(spinResult2.sectorIndex, 0);

  // Additional spin fails due to 0 spins left
  await assert.rejects(
    async () => { await rewards.spinWheel('user123', 'req_new'); },
    (err) => err.code === 'failed-precondition'
  );
});

test('rewardsSubmitRedemption deducts coins and creates pending record', async () => {
  const db = createMockFirestore();
  const rewards = createRewardsService({ db });

  db.store.set('rewards_wallets/user123', {
    availableCoins: 600,
    spentCoins: 0,
    claimedCheckInDates: [],
    availableSpins: 1
  });

  const redResult = await rewards.submitRedemption('user123', 'rdm_opt_1'); // 500 coins cost
  assert.equal(redResult.success, true);
  assert.equal(redResult.availableCoins, 100);
  assert.equal(redResult.spentCoins, 500);
  assert.equal(redResult.redemptionRecord.status, 'PENDING');
});

test('rewardsSubmitReferralCode prevents self referral', async () => {
  const db = createMockFirestore();
  const rewards = createRewardsService({ db });

  const state = await rewards.getState('user123');
  const myCode = state.referralStats.referralCode;

  await assert.rejects(
    async () => { await rewards.submitReferralCode('user123', myCode); },
    (err) => err.code === 'invalid-argument'
  );
});
