# WorkDaily Admin — System Architecture & Integration Specification

## 1. Overview
WorkDaily Admin is a completely independent, standalone Android application (`com.synfusion.workdailyadmin`) designed for platform management, analytics, user moderation, rewards governance, live stream oversight, remote app configuration, support ticketing, and system health monitoring.

It connects directly to the existing WorkDaily Firebase project via secured Admin Cloud Functions and server-enforced role-based access controls (RBAC) without modifying or copying the existing WorkDaily user application (`android-offline-attendance-app`).

---

## 2. Audited Database Schemas & Collections

### 2.1 Existing Collections (Audited from Main Branch)
- **`users/{userId}`**
  - **Fields**: `uid`, `localWorkspaceId`, `displayName`, `email`, `photoUrl`, `phoneNumber`, `gender`, `countryCode`, `currencyCode`, `emailVerified`, `accountStatus` (`ACTIVE`, `SUSPENDED`, `BANNED`), `createdAt`, `updatedAt`, `restrictions` (`chatRestricted`, `liveRestricted`, `postingRestricted`), `isVerified`, `notes` (array).
- **`public_profiles/{userId}`**
  - **Fields**: `uid`, `displayName`, `username`, `usernameLower`, `displayNameLower`, `bio`, `profession`, `photoUrl`, `isPublic`, `isVerified`, `followerCount`, `followingCount`, `gender`, `dateOfBirth`, `countryCode`, `createdAt`, `updatedAt`.
- **`usernames/{username}`**
  - **Fields**: `uid`.
- **`follows/{followerUid_targetUid}`**
  - **Fields**: `followerUid`, `targetUid`, `createdAt`.
- **`blocks/{blockerUid_blockedUid}`**
  - **Fields**: `blockerUid`, `blockedUid`, `timestamp`.
- **`reports/{reportId}`**
  - **Fields**: `id`, `reporterUid`, `targetUid`, `reason`, `details`, `timestamp`, `status` (`PENDING`, `REVIEWING`, `RESOLVED`, `IGNORED`), `assignedAdminUid`, `priority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), `internalNotes`, `resolutionReason`, `evidence`, `resolvedAt`.
- **`conversations/{conversationId}`**
  - **Fields**: `id`, `participants`, `lastMessage`, `lastSenderUid`, `lastTimestamp`, `unreadCounts`, `createdAt`, `updatedAt`.
  - **Subcollection `messages/{messageId}`**: `id`, `conversationId`, `senderUid`, `recipientUid`, `text`, `timestamp`, `status`, `deliveredAt`, `readAt`.
- **`rewards_wallets/{userId}`**
  - **Fields**: `availableCoins`, `pendingCoins`, `lifetimeEarned`, `spentCoins`, `availableSpins`, `referralCode`, `lastCheckInDate`, `claimedCheckInDates`, `currentStreak`, `longestStreak`, `totalCheckInDays`, `claimedMilestoneIds`, `claimedTaskIds`, `referredByCode`, `updatedAt`.
  - **Subcollection `transactions`**: `id`, `title`, `description`, `timestamp`, `amount`, `status`, `type`, `referenceCode`, `source`, `createdAt`, `adminUid`, `reason`.
  - **Subcollection `redemptions`**: `id`, `optionTitle`, `monetaryValue`, `coinsSpent`, `timestamp`, `status` (`PENDING`, `APPROVED`, `REJECTED`), `referenceCode`, `createdAt`, `processedByAdminUid`, `notes`.
  - **Subcollection `referrals`**: `id`, `name`, `status`, `rewardCoins`, `date`, `inviteeUid`, `createdAt`.
- **`referral_codes/{code}`**
  - **Fields**: `uid`, `createdAt`.
- **`leaderboards/{timeframe}/rankings/{userId}`**
  - **Fields**: `uid`, `name`, `xp`, `updatedAt`.
- **`live_rooms/{roomId}`**
  - **Fields**: `roomId`, `hostUserId`, `creatorId`, `title`, `type` (`VIDEO`, `AUDIO`, `TEXT`, `YOUTUBE_WATCH_PARTY`), `isLive`, `isPublic`, `mediaManaged`, `statusText`, `createdAtMillis`, `endedAtMillis`, `endedByAdminUid`, `endReason`.
  - **Subcollection `members/{uid}`**: `uid`, `role`, `joinedAt`.

### 2.2 New Admin & Infrastructure Collections
- **`admin_roles/{uid}`**
  - **Fields**: `uid`, `email`, `role` (`SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `SUPPORT`, `ANALYST`), `assignedBy`, `assignedAt`, `updatedAt`, `mfaEnabled`.
- **`admin_audit_logs/{logId}`**
  - **Fields**: `id`, `adminUid`, `adminEmail`, `action`, `targetType`, `targetId`, `reason`, `details`, `timestamp`, `ipAddress`.
- **`support_tickets/{ticketId}`**
  - **Fields**: `id`, `userUid`, `subject`, `description`, `category`, `status` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), `priority` (`LOW`, `MEDIUM`, `HIGH`), `assignedAdminUid`, `internalNotes`, `replies` (array), `createdAt`, `updatedAt`.
- **`app_config/global`**
  - **Fields**: `minSupportedVersionCode`, `latestVersionCode`, `updateAnnouncementTitle`, `updateAnnouncementMessage`, `forceUpdate`, `maintenanceMode`, `supportWhatsappUrl`, `supportEmail`, `shareMessage`, `announcementBannerText`, `announcementBannerUrl`, `isAnnouncementActive`, `featureFlags` (`liveEnabled`, `socialEnabled`, `rewardsEnabled`), `updatedAt`, `updatedByAdminUid`.
- **`notification_campaigns/{campaignId}`**
  - **Fields**: `id`, `title`, `body`, `targetAudience` (`ALL`, `SELECTED`, `SEGMENT`), `targetUserUids`, `segmentFilter`, `scheduledAt`, `sentAt`, `status` (`DRAFT`, `SCHEDULED`, `SENDING`, `COMPLETED`, `FAILED`), `successCount`, `failureCount`, `createdByAdminUid`, `createdAt`.

---

## 3. Server-Authoritative Roles & Permission Hierarchy

| Role | Permissions |
| :--- | :--- |
| **SUPER_ADMIN** | Full administrative rights: Provision/revoke admin roles, update remote app configuration, purge caches, perform all user/rewards/live/moderation actions. |
| **ADMIN** | User management, session revocation, coin adjustments, redemption processing, notification campaigns, config updates, live room termination. |
| **MODERATOR** | Centralized report queue processing, user suspensions & bans, social content restriction, live stream termination. |
| **SUPPORT** | User profile search, user internal notes, support ticket assignment & response, account status viewing. |
| **ANALYST** | Read-only access to dashboard metrics, user counts, rewards overview, app version distribution, system health indicators. |

---

## 4. Admin Cloud Functions (`functions/admin-service.js`)

All admin operations are mediated by secure Firebase v2 HTTPS Callable Cloud Functions with custom claims verification (`request.auth.token.admin == true`) and role validation against `admin_roles/{uid}`:

1. **`adminGetDashboardStats`**: Aggregates real platform stats (users, active live rooms, reports, coins, version breakdown).
2. **`adminGetUsers`**: Bounded, paginated search and list for user administration.
3. **`adminGetUserDetails`**: Fetches user profile, activity summary, social stats, report history, rewards transactions.
4. **`adminUpdateUserStatus`**: Modifies user account status (`ACTIVE`, `SUSPENDED`, `BANNED`), verification badge, or feature restrictions (`chatRestricted`, `liveRestricted`, `postingRestricted`).
5. **`adminRevokeSessions`**: Uses Firebase Auth Admin SDK `revokeRefreshTokens(uid)` to force sign-out across all devices.
6. **`adminAddUserNote`**: Appends internal admin notes to a user's admin record.
7. **`adminGetReports` / `adminUpdateReportStatus`**: Moderation queue workflow management (`PENDING` -> `REVIEWING` -> `RESOLVED` / `IGNORED`).
8. **`adminGetLiveRooms` / `adminEndLiveRoom`**: Monitors active streams and terminates streams with mandatory reason logging.
9. **`adminAdjustUserCoins`**: Manual coin balance adjustment requiring transaction reference, reason, admin UID, and before/after snapshot.
10. **`adminProcessRedemption`**: Approves or rejects pending coin redemptions with atomic balance synchronization.
11. **`adminSendNotificationCampaign` / `adminGetNotificationCampaigns`**: Scheduled or immediate FCM campaign broadcasts.
12. **`adminGetAppConfig` / `adminUpdateAppConfig`**: Manages app version gates, maintenance mode, and feature toggles.
13. **`adminGetAdminTeam` / `adminAssignRole`**: Super Admin team provisioning and custom claim management (`setCustomUserClaims`).
14. **`adminGetAuditLogs`**: Append-only log retrieval for compliance.
15. **`adminGetSupportTickets` / `adminUpdateSupportTicket`**: Support ticket lifecycle management.
16. **`adminGetSystemHealth`**: Live operational health metrics (Firebase Functions errors, FCM delivery, LiveKit status).

---

## 5. Security & Isolation Guarantee
- **Zero Modification to User App**: `android-offline-attendance-app` is untouched.
- **Package Separation**: Admin package is strictly `com.synfusion.workdailyadmin`.
- **Catch-All Security Rule**: Firestore rules preserve the default `allow read, write: if false;` catch-all deny.
- **Client Security**: No client SDK direct writes are allowed for sensitive administrative collections. All mutations execute in Node.js Firebase Admin SDK callable functions.
