'use strict';

const { HttpsError } = require('firebase-functions/v2/https');

const SPIN_SECTORS = [
  { id: 1, name: 'Safety Helmet', coinReward: 2, iconName: 'helmet', rewardType: 'Coins' },
  { id: 2, name: 'Hammer', coinReward: 4, iconName: 'hammer', rewardType: 'Coins' },
  { id: 3, name: 'Bricks', coinReward: 5, iconName: 'bricks', rewardType: 'Coins' },
  { id: 4, name: 'Toolbox', coinReward: 8, iconName: 'toolbox', rewardType: 'Coins' },
  { id: 5, name: 'Construction Crane', coinReward: 10, iconName: 'crane', rewardType: 'Coins' },
  { id: 6, name: 'Cement Truck', coinReward: 12, iconName: 'truck', rewardType: 'Coins' },
  { id: 7, name: 'Golden Blueprint', coinReward: 20, iconName: 'blueprint', rewardType: 'Coins' },
  { id: 8, name: "Foreman's Pass", coinReward: 0, iconName: 'pass', rewardType: 'BonusSpin' }
];

const REDEMPTION_OPTIONS = {
  'rdm_opt_1': { title: '₹50 Gift Card', coinsCost: 500, monetaryValue: '₹50', iconType: 'giftcard' },
  'rdm_opt_2': { title: '₹100 Gift Card', coinsCost: 1000, monetaryValue: '₹100', iconType: 'giftcard' },
  'rdm_opt_3': { title: '₹200 Gift Card', coinsCost: 2000, monetaryValue: '₹200', iconType: 'giftcard' }
};

const TASKS = {
  'task_1': { title: 'Daily Check-in', coinReward: 5, category: 'DAILY' },
  'task_2': { title: 'Stay Consistent', coinReward: 25, category: 'WEEKLY' },
  'task_3': { title: 'Add Profile Photo', coinReward: 20, category: 'SPECIAL' },
  'task_4': { title: 'Complete Profile', coinReward: 50, category: 'SPECIAL' }
};

function generateReferralCode(uid) {
  const cleanUid = (uid || '').replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
  const suffix = cleanUid.length >= 6 ? cleanUid.slice(-6) : (cleanUid + '7X2988').slice(-6);
  return `WD${suffix}`;
}

function formatDateString(date) {
  const d = date || new Date();
  return d.toISOString().split('T')[0];
}

function createRewardsService({ db, randomSupplier }) {
  const getRandom = randomSupplier || Math.random;

  async function getOrCreateWallet(transaction, uid) {
    const walletRef = db.collection('rewards_wallets').doc(uid);
    const walletDoc = await transaction.get(walletRef);

    if (walletDoc.exists) {
      return { ref: walletRef, data: walletDoc.data() };
    }

    let refCode = generateReferralCode(uid);
    let codeRef = db.collection('referral_codes').doc(refCode);
    let existingCodeDoc = await transaction.get(codeRef);

    // Collision check
    if (existingCodeDoc.exists && existingCodeDoc.data().uid !== uid) {
      const randHex = Math.floor(Math.random() * 899999 + 100000).toString();
      refCode = `WD${randHex}`;
      codeRef = db.collection('referral_codes').doc(refCode);
    }

    const initialWallet = {
      availableCoins: 0,
      pendingCoins: 0,
      lifetimeEarned: 0,
      spentCoins: 0,
      availableSpins: 1,
      referralCode: refCode,
      lastCheckInDate: null,
      claimedCheckInDates: [],
      currentStreak: 0,
      longestStreak: 0,
      totalCheckInDays: 0,
      claimedMilestoneIds: [],
      claimedTaskIds: [],
      referredByCode: null,
      updatedAt: Date.now()
    };

    transaction.set(walletRef, initialWallet);
    transaction.set(codeRef, { uid, createdAt: Date.now() });

    return { ref: walletRef, data: initialWallet };
  }

  async function updateLeaderboards(transaction, uid, xp, displayName) {
    const weeklyRef = db.collection('leaderboards').doc('weekly').collection('rankings').doc(uid);
    const monthlyRef = db.collection('leaderboards').doc('monthly').collection('rankings').doc(uid);

    const entry = {
      uid,
      name: displayName || 'Worker',
      xp: xp || 0,
      updatedAt: Date.now()
    };

    transaction.set(weeklyRef, entry, { merge: true });
    transaction.set(monthlyRef, entry, { merge: true });
  }

  return {
    async getState(uid) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');

      const walletRef = db.collection('rewards_wallets').doc(uid);
      let walletDoc = await walletRef.get();

      if (!walletDoc.exists) {
        await db.runTransaction(async (transaction) => {
          await getOrCreateWallet(transaction, uid);
        });
        walletDoc = await walletRef.get();
      }

      const wallet = walletDoc.data();

      // Fetch subcollections
      const txsSnap = await walletRef.collection('transactions').orderBy('createdAt', 'desc').limit(50).get();
      const transactions = txsSnap.docs.map(doc => doc.data());

      const rdmsSnap = await walletRef.collection('redemptions').orderBy('createdAt', 'desc').limit(50).get();
      const redemptions = rdmsSnap.docs.map(doc => doc.data());

      const refsSnap = await walletRef.collection('referrals').orderBy('createdAt', 'desc').limit(50).get();
      const referrals = refsSnap.docs.map(doc => doc.data());

      return {
        walletSummary: {
          availableCoins: wallet.availableCoins || 0,
          pendingCoins: wallet.pendingCoins || 0,
          lifetimeEarned: wallet.lifetimeEarned || 0,
          spentCoins: wallet.spentCoins || 0
        },
        spinTokens: {
          availableSpins: wallet.availableSpins || 0
        },
        dailyCheckInState: {
          isTodayClaimed: (wallet.claimedCheckInDates || []).includes(formatDateString()),
          todayDate: formatDateString(),
          lastCheckInDate: wallet.lastCheckInDate || null,
          claimedDates: wallet.claimedCheckInDates || []
        },
        streakState: {
          currentStreak: wallet.currentStreak || 0,
          longestStreak: wallet.longestStreak || 0,
          totalCheckInDays: wallet.totalCheckInDays || 0
        },
        claimedMilestoneIds: wallet.claimedMilestoneIds || [],
        claimedTaskIds: wallet.claimedTaskIds || [],
        referralStats: {
          invitedCount: referrals.length,
          rewardedCount: referrals.filter(r => r.status === 'REWARDED').length,
          pendingCount: referrals.filter(r => r.status === 'PENDING').length,
          totalCoinsEarned: referrals.filter(r => r.status === 'REWARDED').reduce((acc, r) => acc + (r.rewardCoins || 0), 0),
          referralCode: wallet.referralCode || generateReferralCode(uid)
        },
        transactions,
        redemptions,
        referrals
      };
    },

    async dailyCheckIn(uid) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');

      const todayStr = formatDateString();

      return await db.runTransaction(async (transaction) => {
        const { ref: walletRef, data: wallet } = await getOrCreateWallet(transaction, uid);

        const claimedDates = wallet.claimedCheckInDates || [];
        if (claimedDates.includes(todayStr)) {
          throw new HttpsError('already-exists', 'Daily check-in already claimed for today.');
        }

        const newClaimedDates = [...claimedDates, todayStr];
        const newTotalDays = newClaimedDates.length;

        // Streak calculation
        let newStreak = 1;
        if (wallet.lastCheckInDate) {
          const lastDate = new Date(wallet.lastCheckInDate);
          const currDate = new Date(todayStr);
          const diffDays = Math.round((currDate - lastDate) / (1000 * 60 * 60 * 24));
          if (diffDays === 1) {
            newStreak = (wallet.currentStreak || 0) + 1;
          } else if (diffDays === 0) {
            newStreak = wallet.currentStreak || 1;
          }
        }

        const newLongestStreak = Math.max(wallet.longestStreak || 0, newStreak);
        const newAvailableCoins = (wallet.availableCoins || 0) + 5;
        const newLifetimeEarned = (wallet.lifetimeEarned || 0) + 5;
        const newAvailableSpins = (wallet.availableSpins || 0) + 1;

        // Auto task 1 update
        const claimedTaskIds = wallet.claimedTaskIds || [];
        if (!claimedTaskIds.includes('task_1')) {
          claimedTaskIds.push('task_1');
        }

        transaction.update(walletRef, {
          availableCoins: newAvailableCoins,
          lifetimeEarned: newLifetimeEarned,
          availableSpins: newAvailableSpins,
          lastCheckInDate: todayStr,
          claimedCheckInDates: newClaimedDates,
          currentStreak: newStreak,
          longestStreak: newLongestStreak,
          totalCheckInDays: newTotalDays,
          claimedTaskIds,
          updatedAt: Date.now()
        });

        // Record transaction
        const txRef = walletRef.collection('transactions').doc();
        const txData = {
          id: txRef.id,
          title: 'Daily Check-in',
          description: `Logged daily check-in for ${todayStr}`,
          timestamp: new Date().toLocaleString(),
          amount: 5,
          status: 'COMPLETED',
          type: 'EARNED',
          referenceCode: `CHK-${Math.floor(10000 + Math.random() * 90000)}`,
          source: 'Daily Check-in',
          createdAt: Date.now()
        };
        transaction.set(txRef, txData);

        await updateLeaderboards(transaction, uid, newLifetimeEarned);

        return {
          success: true,
          availableCoins: newAvailableCoins,
          lifetimeEarned: newLifetimeEarned,
          availableSpins: newAvailableSpins,
          currentStreak: newStreak,
          longestStreak: newLongestStreak,
          totalCheckInDays: newTotalDays,
          todayDate: todayStr
        };
      });
    },

    async claimMilestone(uid, milestoneId) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');
      if (!milestoneId) throw new HttpsError('invalid-argument', 'Milestone ID is required.');

      const milestonesMap = {
        'milestone_7_streak': { coinReward: 20, bonusSpins: 1, title: '7 Day Streak', check: (w) => (w.currentStreak || 0) >= 7 },
        'milestone_30_days': { coinReward: 75, bonusSpins: 0, title: '30 Check-in Days', check: (w) => (w.totalCheckInDays || 0) >= 30 },
        'milestone_180_days': { coinReward: 350, bonusSpins: 0, title: '180 Check-in Days', check: (w) => (w.totalCheckInDays || 0) >= 180 },
        'milestone_365_days': { coinReward: 750, bonusSpins: 3, title: '365 Check-in Days', check: (w) => (w.totalCheckInDays || 0) >= 365 }
      };

      const def = milestonesMap[milestoneId];
      if (!def) throw new HttpsError('not-found', 'Invalid milestone ID.');

      return await db.runTransaction(async (transaction) => {
        const { ref: walletRef, data: wallet } = await getOrCreateWallet(transaction, uid);

        const claimed = wallet.claimedMilestoneIds || [];
        if (claimed.includes(milestoneId)) {
          throw new HttpsError('already-exists', 'Milestone already claimed.');
        }

        if (!def.check(wallet)) {
          throw new HttpsError('failed-precondition', 'Milestone requirements not met.');
        }

        const newClaimed = [...claimed, milestoneId];
        const newCoins = (wallet.availableCoins || 0) + def.coinReward;
        const newLifetime = (wallet.lifetimeEarned || 0) + def.coinReward;
        const newSpins = (wallet.availableSpins || 0) + def.bonusSpins;

        transaction.update(walletRef, {
          availableCoins: newCoins,
          lifetimeEarned: newLifetime,
          availableSpins: newSpins,
          claimedMilestoneIds: newClaimed,
          updatedAt: Date.now()
        });

        const txRef = walletRef.collection('transactions').doc();
        transaction.set(txRef, {
          id: txRef.id,
          title: `${def.title} Milestone`,
          description: `Claimed milestone bonus: ${def.title}`,
          timestamp: new Date().toLocaleString(),
          amount: def.coinReward,
          status: 'CONFIRMED',
          type: 'EARNED',
          referenceCode: `MLS-${Math.floor(10000 + Math.random() * 90000)}`,
          source: 'Milestones',
          createdAt: Date.now()
        });

        await updateLeaderboards(transaction, uid, newLifetime);

        return {
          success: true,
          availableCoins: newCoins,
          availableSpins: newSpins,
          claimedMilestoneIds: newClaimed
        };
      });
    },

    async spinWheel(uid, requestId) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');
      if (!requestId) throw new HttpsError('invalid-argument', 'Request ID is required for idempotency.');

      return await db.runTransaction(async (transaction) => {
        const { ref: walletRef, data: wallet } = await getOrCreateWallet(transaction, uid);

        // Check idempotency
        const spinDocRef = walletRef.collection('processed_spins').doc(requestId);
        const spinDoc = await transaction.get(spinDocRef);
        if (spinDoc.exists) {
          return spinDoc.data().result;
        }

        if ((wallet.availableSpins || 0) < 1) {
          throw new HttpsError('failed-precondition', 'No available spin tokens.');
        }

        // Randomly select sector 0 to 7
        const sectorIndex = Math.floor(getRandom() * SPIN_SECTORS.length) % SPIN_SECTORS.length;
        const sector = SPIN_SECTORS[sectorIndex];

        let newSpins = wallet.availableSpins - 1;
        let newCoins = wallet.availableCoins || 0;
        let newLifetime = wallet.lifetimeEarned || 0;

        if (sector.rewardType === 'Coins') {
          newCoins += sector.coinReward;
          newLifetime += sector.coinReward;
        } else if (sector.rewardType === 'BonusSpin') {
          newSpins += 1;
        }

        transaction.update(walletRef, {
          availableSpins: newSpins,
          availableCoins: newCoins,
          lifetimeEarned: newLifetime,
          updatedAt: Date.now()
        });

        const txRef = walletRef.collection('transactions').doc();
        transaction.set(txRef, {
          id: txRef.id,
          title: 'Spin Wheel Prize',
          description: sector.rewardType === 'Coins'
            ? `Won ${sector.coinReward} coins on ${sector.name}`
            : `Won +1 Free Spin Token on ${sector.name}`,
          timestamp: new Date().toLocaleString(),
          amount: sector.coinReward,
          status: 'CONFIRMED',
          type: 'EARNED',
          referenceCode: `SPN-${Math.floor(10000 + Math.random() * 90000)}`,
          source: 'Spin Wheel',
          createdAt: Date.now()
        });

        const result = {
          sectorIndex,
          segment: sector,
          coinsEarned: sector.coinReward,
          availableCoins: newCoins,
          availableSpins: newSpins
        };

        transaction.set(spinDocRef, {
          requestId,
          result,
          createdAt: Date.now()
        });

        await updateLeaderboards(transaction, uid, newLifetime);

        return result;
      });
    },

    async claimTask(uid, taskId) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');
      if (!taskId || !TASKS[taskId]) throw new HttpsError('invalid-argument', 'Valid task ID is required.');

      const task = TASKS[taskId];

      return await db.runTransaction(async (transaction) => {
        const { ref: walletRef, data: wallet } = await getOrCreateWallet(transaction, uid);

        const claimed = wallet.claimedTaskIds || [];
        if (claimed.includes(taskId)) {
          throw new HttpsError('already-exists', 'Task already claimed.');
        }

        const newClaimed = [...claimed, taskId];
        const newCoins = (wallet.availableCoins || 0) + task.coinReward;
        const newLifetime = (wallet.lifetimeEarned || 0) + task.coinReward;

        transaction.update(walletRef, {
          availableCoins: newCoins,
          lifetimeEarned: newLifetime,
          claimedTaskIds: newClaimed,
          updatedAt: Date.now()
        });

        const txRef = walletRef.collection('transactions').doc();
        transaction.set(txRef, {
          id: txRef.id,
          title: 'Task Reward',
          description: `Completed task: ${task.title}`,
          timestamp: new Date().toLocaleString(),
          amount: task.coinReward,
          status: 'CONFIRMED',
          type: 'EARNED',
          referenceCode: `TSK-${Math.floor(10000 + Math.random() * 90000)}`,
          source: 'Tasks',
          createdAt: Date.now()
        });

        await updateLeaderboards(transaction, uid, newLifetime);

        return {
          success: true,
          taskId,
          availableCoins: newCoins,
          claimedTaskIds: newClaimed
        };
      });
    },

    async submitReferralCode(uid, inputCode) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');
      const trimmedCode = (inputCode || '').trim().toUpperCase();
      if (!trimmedCode) throw new HttpsError('invalid-argument', 'Referral code is required.');

      return await db.runTransaction(async (transaction) => {
        const { ref: walletRef, data: wallet } = await getOrCreateWallet(transaction, uid);

        if (wallet.referredByCode) {
          throw new HttpsError('already-exists', 'You have already applied a referral code.');
        }

        if (wallet.referralCode === trimmedCode) {
          throw new HttpsError('invalid-argument', 'You cannot enter your own referral code.');
        }

        const codeDocRef = db.collection('referral_codes').doc(trimmedCode);
        const codeDoc = await transaction.get(codeDocRef);
        if (!codeDoc.exists) {
          throw new HttpsError('not-found', 'Invalid referral code.');
        }

        const inviterUid = codeDoc.data().uid;
        if (inviterUid === uid) {
          throw new HttpsError('invalid-argument', 'You cannot refer yourself.');
        }

        transaction.update(walletRef, {
          referredByCode: trimmedCode,
          updatedAt: Date.now()
        });

        // Record pending referral on inviter's wallet
        const inviterWalletRef = db.collection('rewards_wallets').doc(inviterUid);
        const refItemRef = inviterWalletRef.collection('referrals').doc(uid);
        transaction.set(refItemRef, {
          id: uid,
          name: 'Invited Friend',
          status: 'PENDING',
          rewardCoins: 100,
          date: new Date().toLocaleDateString(),
          inviteeUid: uid,
          createdAt: Date.now()
        });

        return {
          success: true,
          referralCode: trimmedCode
        };
      });
    },

    async submitRedemption(uid, optionId, requestId) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');
      const option = REDEMPTION_OPTIONS[optionId];
      if (!option) throw new HttpsError('invalid-argument', 'Invalid redemption option.');

      const reqId = requestId || `rdm_${Date.now()}_${Math.floor(Math.random() * 100000)}`;

      return await db.runTransaction(async (transaction) => {
        const { ref: walletRef, data: wallet } = await getOrCreateWallet(transaction, uid);

        // Check idempotency
        const procRef = walletRef.collection('processed_redemptions').doc(reqId);
        const procDoc = await transaction.get(procRef);
        if (procDoc.exists) {
          return procDoc.data().result;
        }

        if ((wallet.availableCoins || 0) < option.coinsCost) {
          throw new HttpsError('failed-precondition', 'Insufficient coin balance for redemption.');
        }

        const newAvailable = wallet.availableCoins - option.coinsCost;
        const newSpent = (wallet.spentCoins || 0) + option.coinsCost;

        transaction.update(walletRef, {
          availableCoins: newAvailable,
          spentCoins: newSpent,
          updatedAt: Date.now()
        });

        const rdmRef = walletRef.collection('redemptions').doc();
        const rdmRecord = {
          id: rdmRef.id,
          optionTitle: option.title,
          monetaryValue: option.monetaryValue,
          coinsSpent: option.coinsCost,
          timestamp: new Date().toLocaleString(),
          status: 'PENDING',
          referenceCode: `RDM-${Math.floor(10000 + Math.random() * 90000)}`,
          createdAt: Date.now()
        };
        transaction.set(rdmRef, rdmRecord);

        const txRef = walletRef.collection('transactions').doc();
        transaction.set(txRef, {
          id: txRef.id,
          title: 'Reward Redemption',
          description: `Redeemed ${option.title} (${option.monetaryValue})`,
          timestamp: new Date().toLocaleString(),
          amount: -option.coinsCost,
          status: 'PENDING',
          type: 'SPENT',
          referenceCode: rdmRecord.referenceCode,
          source: 'Redemption Center',
          createdAt: Date.now()
        });

        const result = {
          success: true,
          redemptionRecord: rdmRecord,
          availableCoins: newAvailable,
          spentCoins: newSpent
        };

        transaction.set(procRef, {
          requestId: reqId,
          result,
          createdAt: Date.now()
        });

        return result;
      });
    },

    async getLeaderboard(uid, timeframe) {
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication required.');

      const validTimeframe = (timeframe === 'MONTHLY') ? 'monthly' : 'weekly';
      const rankingsRef = db.collection('leaderboards').doc(validTimeframe).collection('rankings');

      const snapshot = await rankingsRef.orderBy('xp', 'desc').limit(20).get();
      let topUsers = snapshot.docs.map((doc, idx) => ({
        id: doc.id,
        rank: idx + 1,
        name: doc.data().name || 'Worker',
        xp: doc.data().xp || 0,
        isCurrentUser: doc.id === uid
      }));

      // Find authenticated user's actual rank & score if not in top 20
      let currentUserItem = topUsers.find(u => u.isCurrentUser);
      if (!currentUserItem) {
        const userDoc = await rankingsRef.doc(uid).get();
        if (userDoc.exists) {
          const userXp = userDoc.data().xp || 0;
          const higherSnap = await rankingsRef.where('xp', '>', userXp).get();
          const userRank = higherSnap.size + 1;
          currentUserItem = {
            id: uid,
            rank: userRank,
            name: userDoc.data().name || 'You',
            xp: userXp,
            isCurrentUser: true
          };
          topUsers.push(currentUserItem);
        }
      }

      return {
        timeframe: validTimeframe.toUpperCase(),
        leaderboard: topUsers
      };
    }
  };
}

module.exports = { createRewardsService, SPIN_SECTORS, REDEMPTION_OPTIONS, TASKS };
