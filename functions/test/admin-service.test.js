'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { createAdminService, ROLES } = require('../admin-service');

function createMockDb() {
  const collections = new Map();

  function getCollection(name) {
    if (!collections.has(name)) {
      collections.set(name, new Map());
    }
    return collections.get(name);
  }

  function createColObject(name) {
    const colMap = getCollection(name);
    return {
      doc(id) {
        const docId = id || `doc_${Math.random().toString(36).substring(2, 9)}`;
        return {
          id: docId,
          async get() {
            const data = colMap.get(docId);
            return {
              id: docId,
              exists: !!data,
              data: () => data,
              getString: (field) => (data && data[field]) || null
            };
          },
          async set(data, options = {}) {
            if (options.merge && colMap.has(docId)) {
              colMap.set(docId, { ...colMap.get(docId), ...data });
            } else {
              colMap.set(docId, data);
            }
          },
          async update(data) {
            if (!colMap.has(docId)) throw new Error(`Document ${docId} not found`);
            colMap.set(docId, { ...colMap.get(docId), ...data });
          },
          collection(subName) {
            return createColObject(`${name}/${docId}/${subName}`);
          }
        };
      },
      where() {
        return {
          limit() {
            return {
              async get() {
                return { docs: [], size: 0 };
              }
            };
          },
          async get() {
            return { docs: [], size: 0 };
          }
        };
      },
      limit() {
        return {
          async get() {
            return { docs: [], size: 0 };
          }
        };
      },
      async get() {
        const docs = Array.from(colMap.entries()).map(([id, data]) => ({
          id,
          data: () => data
        }));
        return { docs, size: docs.length };
      }
    };
  }

  return {
    _collections: collections,
    collection(name) {
      return createColObject(name);
    },
    async runTransaction(updateFunction) {
      const transaction = {
        async get(docRef) {
          return await docRef.get();
        },
        set(docRef, data, options = {}) {
          docRef.set(data, options);
        }
      };
      return await updateFunction(transaction);
    }
  };
}

function makeAdminReq(uid = 'admin_1', role = ROLES.SUPER_ADMIN) {
  return {
    auth: {
      uid,
      token: { email: `${uid}@workdaily.com`, admin: true, role }
    }
  };
}

test('adjustUserCoins clamps negative adjustments to zero and records actual applied amount', async () => {
  const db = createMockDb();
  const adminService = createAdminService({ db });

  // Pre-seed wallet with 50 coins
  await db.collection('rewards_wallets').doc('user_123').set({
    availableCoins: 50,
    lifetimeEarned: 100
  });

  // Seed admin role in admin_roles doc
  await db.collection('admin_roles').doc('admin_1').set({
    uid: 'admin_1',
    role: ROLES.ADMIN
  });

  // Request -100 adjustment (overdraft attempt)
  const req = makeAdminReq('admin_1', ROLES.ADMIN);
  const res = await adminService.adjustUserCoins(req, {
    targetUid: 'user_123',
    amount: -100,
    reason: 'Penalty'
  });

  assert.equal(res.success, true);
  assert.equal(res.beforeCoins, 50);
  assert.equal(res.afterCoins, 0);
  assert.equal(res.actualAppliedAdjustment, -50);

  // Check updated wallet
  const walletDoc = await db.collection('rewards_wallets').doc('user_123').get();
  assert.equal(walletDoc.data().availableCoins, 0);

  // Check audit log recorded outside transaction
  const auditLogs = await db.collection('admin_audit_logs').get();
  assert.equal(auditLogs.size, 1);
  assert.equal(auditLogs.docs[0].data().details.amountAdjusted, -50);
});

test('updateUserStatus throws not-found if target user does not exist', async () => {
  const db = createMockDb();
  const adminService = createAdminService({ db });

  await db.collection('admin_roles').doc('admin_1').set({
    uid: 'admin_1',
    role: ROLES.MODERATOR
  });

  const req = makeAdminReq('admin_1', ROLES.MODERATOR);

  await assert.rejects(
    async () => {
      await adminService.updateUserStatus(req, {
        targetUid: 'non_existent_user',
        accountStatus: 'SUSPENDED',
        reason: 'Violation'
      });
    },
    (err) => {
      assert.equal(err.code, 'not-found');
      return true;
    }
  );
});
