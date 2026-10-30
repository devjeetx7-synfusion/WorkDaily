'use strict';

const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { FieldPath } = require('firebase-admin/firestore');
const { defineSecret, defineString } = require('firebase-functions/params');
const { createTokenService, TokenError } = require('./token-service');
const { createLiveMediaService } = require('./live-media-service');
const { createRewardsService } = require('./rewards-service');
const { createAdminService } = require('./admin-service');
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
  isMember: async (uid, roomId) => {
    const member = await getFirestore().collection('live_rooms').doc(roomId).collection('members').doc(uid).get();
    return member.exists && member.data().uid === uid;
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

// Bounded server-owned maintenance with a persisted cursor to avoid starving later rooms.
// Every room remains eligible for a later cycle; no room identifiers or secrets are logged.
exports.reconcileLiveMediaRooms = onSchedule({
  schedule: 'every 5 minutes', secrets: [apiKey, apiSecret], timeoutSeconds: 540,
  maxInstances: 1,
}, async () => {
  const db = getFirestore();
  const cursorRef = db.collection('live_media_maintenance').doc('cursor');
  const cursorSnapshot = await cursorRef.get();
  const cursorId = cursorSnapshot.exists ? cursorSnapshot.data().lastRoomId : null;
  let query = db.collection('live_rooms').where('mediaManaged', '==', true)
    .orderBy(FieldPath.documentId()).limit(25);
  if (typeof cursorId === 'string' && cursorId) query = query.startAfter(cursorId);
  const page = await query.get();
  let failures = 0;
  for (const doc of page.docs) {
    try { await media.reconcile(doc.id); }
    catch (_) { failures++; /* this room remains eligible in the next sweep */ }
    await cursorRef.set({ lastRoomId: doc.id });
  }
  if (page.size < 25) await cursorRef.set({ lastRoomId: null });
  if (failures) throw new Error(`Live media reconciliation failed for ${failures} room(s)`);
});

// ---------------------------------------------------------------------------------------
// WorkDaily Rewards System Cloud Functions
// ---------------------------------------------------------------------------------------
const rewards = createRewardsService({
  get db() { return getFirestore(); }
});

function rewardsCallable(handler) {
  return onCall({ timeoutSeconds: 20 }, async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError('unauthenticated', 'User must be authenticated.');
    }
    return await handler(request.auth.uid, request.data || {});
  });
}

exports.rewardsGetState = rewardsCallable((uid) => rewards.getState(uid));
exports.rewardsDailyCheckIn = rewardsCallable((uid) => rewards.dailyCheckIn(uid));
exports.rewardsClaimMilestone = rewardsCallable((uid, data) => rewards.claimMilestone(uid, data.milestoneId));
exports.rewardsSpinWheel = rewardsCallable((uid, data) => rewards.spinWheel(uid, data.requestId));
exports.rewardsClaimTask = rewardsCallable((uid, data) => rewards.claimTask(uid, data.taskId));
exports.rewardsSubmitReferralCode = rewardsCallable((uid, data) => rewards.submitReferralCode(uid, data.referralCode));
exports.rewardsSubmitRedemption = rewardsCallable((uid, data) => rewards.submitRedemption(uid, data.optionId, data.requestId));
exports.rewardsGetLeaderboard = rewardsCallable((uid, data) => rewards.getLeaderboard(uid, data.timeframe));

// ---------------------------------------------------------------------------------------
// WorkDaily Admin Management Cloud Functions
// ---------------------------------------------------------------------------------------
const adminService = createAdminService({
  get db() { return getFirestore(); }
});

function adminCallable(handler) {
  return onCall({ timeoutSeconds: 30 }, async (request) => {
    return await handler(request);
  });
}

exports.adminGetDashboardStats = adminCallable((req) => adminService.getDashboardStats(req));
exports.adminGetUsers = adminCallable((req) => adminService.getUsers(req, req.data || {}));
exports.adminGetUserDetails = adminCallable((req) => adminService.getUserDetails(req, req.data || {}));
exports.adminUpdateUserStatus = adminCallable((req) => adminService.updateUserStatus(req, req.data || {}));
exports.adminRevokeSessions = adminCallable((req) => adminService.revokeSessions(req, req.data || {}));
exports.adminAddUserNote = adminCallable((req) => adminService.addUserNote(req, req.data || {}));
exports.adminGetReports = adminCallable((req) => adminService.getReports(req, req.data || {}));
exports.adminUpdateReportStatus = adminCallable((req) => adminService.updateReportStatus(req, req.data || {}));
exports.adminGetLiveRooms = adminCallable((req) => adminService.getLiveRooms(req, req.data || {}));
exports.adminEndLiveRoom = adminCallable((req) => adminService.endLiveRoom(req, req.data || {}));
exports.adminAdjustUserCoins = adminCallable((req) => adminService.adjustUserCoins(req, req.data || {}));
exports.adminProcessRedemption = adminCallable((req) => adminService.processRedemption(req, req.data || {}));
exports.adminSendNotificationCampaign = adminCallable((req) => adminService.sendNotificationCampaign(req, req.data || {}));
exports.adminGetNotificationCampaigns = adminCallable((req) => adminService.getNotificationCampaigns(req));
exports.adminGetAppConfig = adminCallable((req) => adminService.getAppConfig(req));
exports.adminUpdateAppConfig = adminCallable((req) => adminService.updateAppConfig(req, req.data || {}));
exports.adminGetAdminTeam = adminCallable((req) => adminService.getAdminTeam(req));
exports.adminAssignRole = adminCallable((req) => adminService.assignAdminRole(req, req.data || {}));
exports.adminGetAuditLogs = adminCallable((req) => adminService.getAuditLogs(req, req.data || {}));
exports.adminGetSupportTickets = adminCallable((req) => adminService.getSupportTickets(req, req.data || {}));
exports.adminUpdateSupportTicket = adminCallable((req) => adminService.updateSupportTicket(req, req.data || {}));
exports.adminGetSystemHealth = adminCallable((req) => adminService.getSystemHealth(req));
