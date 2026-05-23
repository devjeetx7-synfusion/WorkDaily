# DailyWork Project Analysis

## 1. APP OVERVIEW

**What this app does:**
DailyWork is a dual-mode application (Web and Native Android) designed to help individuals and contractors track daily labor, attendance, earnings, and advance payments. It effectively acts as a digital ledger and passbook, replacing traditional paper-based record keeping.

**User Roles:**
1.  **Contractor Mode:** For users who manage multiple laborers. They can add workers, define custom work types, mark daily attendance in batches, add advance payments, and generate monthly passbooks (PDF/WhatsApp) for each worker.
2.  **Personal Mode:** For individuals tracking their own daily wages. They can log daily check-ins (full day, half day, absent, overtime), record received advance payments, and generate their own monthly PDF passbook.

**Core Features List:**
*   Role-based dashboards (Contractor vs. Personal).
*   Worker Management (CRUD operations, Custom Work Types).
*   Batch Attendance Marking (Contractor Calendar).
*   Individual Attendance Marking (Personal Dashboard).
*   Financial Management (Gross earnings vs. Advance deductions -> Net Payable).
*   PDF Generation & WhatsApp Sharing for worker passbooks.
*   Premium Subscriptions (Paywall for exports, >10 workers).
*   Real-time Sync (Firebase Firestore & Auth).
*   Cross-Platform (React Web app & Native Android Jetpack Compose app).

---

## 2. SCREEN STRUCTURE

### Web App Screens (`src/pages/`)
*   **Login (`Login.tsx`)**
    *   *Purpose:* Authentication entry point.
    *   *Data Shows:* Google/Email login options.
    *   *Firebase:* Writes to `users` collection on first login.
    *   *Flow:* Opens from root -> Navigates to `RoleSelection` or `Dashboard`.
*   **RoleSelection (`RoleSelection.tsx`)**
    *   *Purpose:* Onboarding step to select user type.
    *   *Data Shows:* "Contractor" or "Personal" options.
    *   *Firebase:* Updates `role` field in `users` collection.
    *   *Flow:* Opens from `Login` -> Navigates to `Dashboard`.
*   **Dashboard (`Dashboard.tsx` -> `ContractorDashboard.tsx` / `PersonalDashboard.tsx`)**
    *   *Purpose:* Central hub.
    *   *Data Shows:* Monthly summaries (earnings, paid, pending), quick actions, attendance snapshot.
    *   *Firebase:* Reads `workers`, reads/writes `attendance`.
    *   *Flow:* Opens from Login/RoleSelection -> Navigates to `WorkersPage`, `CalendarPage`, `WorkerDetail`, `PersonalPassbook`.
*   **WorkersPage (`WorkersPage.tsx`)**
    *   *Purpose:* Contractor's worker list.
    *   *Data Shows:* List of workers with basic details, search/filter.
    *   *Firebase:* Reads/writes `workers`. Reads `workTypes`.
    *   *Flow:* Opens from `Dashboard` -> Navigates to `WorkerDetail`.
*   **WorkerDetail (`WorkerDetail.tsx`)**
    *   *Purpose:* Individual worker passbook/ledger.
    *   *Data Shows:* Profile info, attendance rate, monthly financial summary, daily logs.
    *   *Firebase:* Reads `workers`, reads `attendance`.
    *   *Flow:* Opens from `WorkersPage` -> PDF/WhatsApp export actions.
*   **PersonalPassbook (`PersonalPassbook.tsx`)**
    *   *Purpose:* Individual user's ledger.
    *   *Data Shows:* Monthly financial summary, daily logs.
    *   *Firebase:* Reads `attendance`, reads `users`.
    *   *Flow:* Opens from `PersonalDashboard` -> PDF export action.
*   **CalendarPage (`CalendarPage.tsx` / `ContractorCalendar.tsx`)**
    *   *Purpose:* Calendar view for marking attendance.
    *   *Data Shows:* Monthly grid with color-coded statuses.
    *   *Firebase:* Reads/writes `attendance`. Reads `workers` (for contractor).
*   **StatsPage (`StatsPage.tsx` / `ContractorStats.tsx`)**
    *   *Purpose:* Analytics and charts.
    *   *Data Shows:* Recharts graphs of attendance and payments over time.
    *   *Firebase:* Reads `attendance`, reads `workers`.
*   **SettingsPage (`SettingsPage.tsx`)**
    *   *Purpose:* Profile and app configuration.
    *   *Data Shows:* User profile, language preference, role switch option.
    *   *Firebase:* Reads/writes `users`.
*   **PremiumPage (`PremiumPage.tsx`)**
    *   *Purpose:* Subscription pricing and upgrade flow.
    *   *Data Shows:* Pricing tiers.
    *   *Firebase:* Writes `isPremium` to `users`.

### Android App Screens (`android-app/.../ui/`)
*   **MainActivity**: Navigation Host.
*   **LoginScreen / RoleSelectionScreen**: Matches web onboarding.
*   **DashboardScreen**: Bottom Navigation host, shows stats.
*   **WorkersScreen**: List of workers (Contractor).
*   **WorkerDetailScreen**: Native passbook view for a worker.
*   **PassbookScreen**: Native passbook view for personal role.
*   **CalendarScreen**: Native Grid calendar for batch/individual marking.
*   **StatsScreen**: Native charting (usually via Compose Canvas or library).
*   **SettingsScreen**: Native preferences.
*   **PremiumScreen**: Native subscription screen.

---

## 3. FIREBASE DATABASE STRUCTURE

The database is a flat NoSQL structure designed to avoid N+1 queries by keeping logs at the root level rather than as subcollections.

**Collection: `users`**
*   `uid` (string) - Document ID
*   `name` (string)
*   `email` (string)
*   `role` (string) - "contractor" or "personal"
*   `daily_wage` (number) - Default wage for personal users
*   `isPremium` (boolean) - Subscription status
*   `language` (string) - e.g., "en", "hi"
*   `created_at` (timestamp)

**Collection: `workers`**
*   `id` (string) - Document ID (auto-generated)
*   `contractorId` (string) - Foreign key to `users.uid`
*   `name` (string)
*   `phone` (string)
*   `aadhar` (string)
*   `age` (number)
*   `workType` (string) - e.g., "Labour", "Electrician"
*   `wage` (number) - Daily wage for this specific worker
*   `created_at` (timestamp)

**Collection: `attendance`**
*   `id` (string) - Document ID (Format: `{user_id}_{YYYY-MM-DD}` or `{user_id}_{YYYY-MM-DD}_advance`)
*   `user_id` (string) - Either personal `users.uid` OR `"worker_" + workers.id`
*   `contractorId` (string, optional) - Present if marked by a contractor
*   `date` (string) - Format: "YYYY-MM-DD"
*   `status` (string) - "present", "absent", or "advance"
*   `type` (string, optional) - "full" or "half" (if status is present)
*   `advance_amount` (number, optional) - Used if status is "advance"
*   `overtime_hours` (number, optional)
*   `reason` (string, optional) - e.g., "sick", "holiday" (if absent)
*   `note` (string, optional)
*   `daily_wage` (number, optional) - Snapshot of wage at the time of marking
*   `timestamp` (timestamp)

**Collection: `workTypes`**
*   `id` (string) - Auto-generated
*   `name` (string)
*   `createdBy` (string) - "system" or `users.uid`
*   `isSystem` (boolean)
*   `created_at` (timestamp)

---

## 4. DATA FLOW

1.  **Authentication & Profile Setup:**
    *   Firebase Auth handles login. `AuthContext` (Web) or `AuthViewModel` (Android) listens to auth state changes.
    *   Upon auth, the app fetches the `users` document to determine `role` and `isPremium` status.
2.  **Fetching Data (Web):**
    *   Most React pages use `useEffect` hooks with standard one-time `getDocs` queries to fetch data when the component mounts.
    *   *Example:* `ContractorDashboard` fetches all `workers` where `contractorId == user.uid`, then fetches ALL `attendance` for that contractor. It then manually loops through the arrays to calculate totals (Total Paid, Pending Amount).
3.  **Real-Time Data (Android):**
    *   The Android app heavily utilizes `addSnapshotListener` inside ViewModels (e.g., `DashboardViewModel.kt`).
    *   This provides real-time UI synchronization. Changes made in Firestore instantly reflect in the `StateFlow` backing the Compose UI.
4.  **Heavy Reads:**
    *   The most significant data pull happens on Dashboard and Stats screens.
    *   To calculate "Pending Amount" or "Gross Earned", the client pulls the `attendance` collection.
    *   In the web app's `ContractorDashboard`, it fetches *all* attendance records for the contractor and filters by month in memory:
        ```javascript
        const attQ = query(collection(db, "attendance"), where("contractorId", "==", user.uid));
        // Client-side filtering: if (data.date.startsWith(monthStr)) ...
        ```

---

## 5. PERFORMANCE ANALYSIS

1.  **Over-fetching & Memory Filtering (Critical):**
    *   *Issue:* Screens like `ContractorDashboard` and `DashboardViewModel` (Android) fetch the *entire* historical `attendance` collection for a user/contractor just to calculate the *current* month's stats.
    *   *Impact:* As a user uses the app for months/years, the document read count will explode, causing slow load times, high memory usage, and increased Firebase billing.
2.  **Missing Pagination:**
    *   *Issue:* `WorkersPage.tsx`, `History.tsx`, and `ContractorHistory.tsx` load all documents without limits or pagination.
    *   *Impact:* Unbounded list growth leading to UI lag and unnecessary document reads.
3.  **Unnecessary Listeners (Android):**
    *   *Issue:* `DashboardViewModel` sets up snapshot listeners on the *entire* `attendance` collection.
    *   *Impact:* Every time an old record is updated, the entire listener fires, transmitting all data again, and triggering a full loop recalculation of stats.
4.  **Lack of Aggregated Data:**
    *   *Issue:* "Pending Amount" requires iterating over every single day a worker has worked and subtracting every advance they have taken. There is no running balance stored on the worker document.

---

## 6. IMPROVEMENT SUGGESTIONS

### 1. Query Optimization (High Priority)
Instead of fetching all attendance history and filtering by `startsWith("YYYY-MM")` in memory, Firebase queries should use range filters on the `date` string.

**Current:**
```javascript
const attQ = query(collection(db, "attendance"), where("contractorId", "==", user.uid));
```
**Suggested:**
```javascript
const attQ = query(
  collection(db, "attendance"),
  where("contractorId", "==", user.uid),
  where("date", ">=", `${yearStr}-${monthStr}-01`),
  where("date", "<=", `${yearStr}-${monthStr}-31`)
);
```
*(Note: Requires a composite index in Firestore on `contractorId` Ascending, `date` Ascending).*

### 2. Database Structure Adjustments (Aggregations)
To fix the heavy read loops for calculating balances, implement a summary collection or fields updated via Cloud Functions or Batch Writes.

**Suggested Approach:**
Add a `monthly_stats` subcollection or root collection:
`stats/{contractorId}_worker_{workerId}_{YYYY-MM}`
Fields:
*   `total_man_days`
*   `gross_earned`
*   `total_advance`
*   `net_payable`

When marking attendance, use a Firestore Transaction to update the daily log *and* increment/decrement the monthly summary document. Dashboards only need to read 1 summary document instead of 30+ daily log documents.

### 3. Pagination Implementation
*   **Web:** Use `limit()` and `startAfter()` in queries for `WorkersPage` and `History`. Implement an "infinite scroll" or "Load More" button.
*   **Android:** Use Jetpack Paging 3 library combined with Firestore Paging source for lists.

### 4. Optimize Android Listeners
In `DashboardViewModel.kt`, adjust the `attendanceListener` to only listen to the current month's records.
```kotlin
attendanceListener = db.collection("attendance")
    .whereEqualTo("contractorId", user.uid)
    .whereGreaterThanOrEqualTo("date", "$currentMonthStr-01")
    .whereLessThanOrEqualTo("date", "$currentMonthStr-31")
    .addSnapshotListener { ... }
```

### 5. Document Merging (Advances)
Currently, saving an advance payment fetches existing advances and manually adds them before saving. Using Firestore's `FieldValue.increment(amount)` with `SetOptions.merge()` would be safer against race conditions and require fewer reads.
