# Phase 9: Deliverables — Offline Attendance User App Report

This report outlines the complete structural and technical layout of the newly designed offline attendance application inside the WorkDaily repository. The offline variant operates under package `com.synfusion.attendance`, ensuring 100% isolation from the original Firebase/AdMob-enabled WorkDaily User App.

---

## 1. Repository Analysis Report
The existing WorkDaily User App was scanned to identify how data, settings, navigation, and user states are handled.
- **Core Observation:** The original application heavily relies on Firebase (Auth, Firestore, Cloud Storage) and Google AdMob (AdCoordinator, RewardedAdManager) for online persistence and point-of-use feature gating.
- **Offline Adaptation Strategy:** We are creating a fully native offline Android application under the `android-offline-attendance-app/` directory (module name `:app`). All Firebase services are replaced with an isolated **Room Database** and **DataStore** preferences. The AdMob dependency is completely stripped out, and previously gated screens/features are permanently unlocked for seamless offline execution.

---

## 2. Screen Inventory
Every user-facing screen from the original User App is cloned exactly to maintain a visually identical design:
1. **SplashScreen:** Plays visual entrance animation and checks local DataStore preferences. If `splashEnabled` is false, it navigates directly to local Login or Dashboard.
2. **LanguageSelectionScreen:** Offers preferred language options (English, Hindi) stored in local preferences.
3. **LoginScreen:** Standard login matching original styling but performing offline authentication using a local secure profile pin or name-based login.
4. **RoleSelectionScreen:** Prompts selecting between "Personal" and "Contractor" mode and saves to local preference.
5. **DashboardScreen:** Centered pager containing 4 main bottom navigation tabs:
   - **Dashboard (Home Tab):** Displays user profile, headers, stats cards (Total Workers, Today Present, Today Absent, Earnings/Paid/Pending counts), quick action items, and daily attendance logs.
   - **Calendar Tab:** Full interactive calendar view to add/edit attendance, notes, and advance payments for selected dates.
   - **Stats Tab:** Visual performance and cost summaries, attendance breakdown rate, and worker list summary.
   - **Settings Tab:** Vertical list displaying profile card (with bottom sheet edit action), reminders toggle, splash enable toggle, language switcher, role manager, support & tutorials, privacy policy, and logout flow.
6. **PassbookScreen:** Digital ledger tracking gross earnings, advance deductions, and net payables with native PDF report generation.
7. **WorkersScreen:** Manage contractor helpers and workers. Features search, sorting, and inline deletion.
8. **WorkerDetailScreen:** Detail card for an individual worker including custom work types, daily wage, and quick shortcut to their specific calendar.
9. **WorkerCalendarScreen:** Dedicated attendance calendar view for a contractor's specific worker.
10. **WorkerHistoryScreen:** Month-by-month financial ledger and attendance stats of all contractor workers.
11. **OnboardingScreen:** Multi-page carousel visual onboarding introducing features with smooth animations.

---

## 3. Feature Inventory
All user features are fully preserved in this offline adaptation:
- **Local Authentication:** Sign-in via name/pin stored securely in DataStore.
- **Attendance Logging:** Mark present (Full/Half Day), mark absent, and overtime tracking.
- **Financial Ledger & Advance Deduction:** Record advance payments and automatically deduct them from gross wages.
- **Custom Work Types:** Define custom roles and daily wage parameters for workers.
- **Native PDF Passbook Export:** Generate native Android PDF documents containing clean monthly attendance spreadsheets.
- **Offline Mode:** Zero reliance on Cloud services; instantaneous DB writes.

---

## 4. Navigation Map
The module uses Jetpack Compose Navigation for a seamless back stack experience:
```
[Splash Screen]
       │ (Finished / Preference Check)
       ▼
[Language Selection]
       │
       ▼
[Login Screen] (Offline Sign-In)
       │
       ▼
[Role Selection] (Personal vs. Contractor)
       │
       ▼
[Dashboard Screen] (Horizontal Pager: 4 tabs)
  ├── Tab 0: Home / Quick Actions ──► [Workers List] ──► [Worker Detail] ──► [Worker Calendar]
  │                             └──► [Passbook]
  ├── Tab 1: Interactive Calendar
  ├── Tab 2: Stats breakdown ──► [Worker History Ledger]
  └── Tab 3: Settings ──► [Support Pages] / [Privacy Policy] / [Edit Profile Sheet]
```

---

## 5. Database Schema (Room)
The offline SQLite database schema consists of the following key tables:

### A. `users` Table
Stores local profiles to simulate multi-user accounts offline.
- `id`: TEXT (Primary Key)
- `name`: TEXT
- `phoneNumber`: TEXT
- `email`: TEXT
- `role`: TEXT (contractor or personal)
- `dailyWage`: REAL
- `createdAt`: INTEGER

### B. `workers` Table
Stores contractor's registered workers.
- `id`: TEXT (Primary Key)
- `name`: TEXT
- `phone`: TEXT
- `aadhaar`: TEXT (Optional)
- `dailyWage`: REAL
- `age`: INTEGER
- `workType`: TEXT (Mistry, Helper, etc.)
- `createdAt`: INTEGER

### C. `attendance` Table
Stores daily attendance records for both workers and personal logs.
- `id`: TEXT (Primary Key, e.g. `workerId_date` or `personal_date`)
- `workerId`: TEXT (Nullable, null for personal logs)
- `date`: TEXT (YYYY-MM-DD)
- `status`: TEXT (present or absent)
- `type`: TEXT (full or half)
- `reason`: TEXT (Optional)
- `overtimeHours`: INTEGER
- `note`: TEXT
- `advanceAmount`: REAL
- `wage`: REAL
- `createdAt`: INTEGER

### D. `summaries` Table
Pre-calculated statistics per month to ensure instantaneous, responsive loads.
- `monthId`: TEXT (Primary Key, e.g., `YYYY-MM`)
- `role`: TEXT
- `totalPresent`: REAL
- `totalWages`: REAL
- `totalAdvance`: REAL
- `todayPresentCount`: REAL
- `todayAbsentCount`: REAL

---

## 6. Folder Structure
The implementation resides inside `android-offline-attendance-app/app` and follows MVVM Clean Architecture:
```
app/
├── build.gradle.kts
└── src/
    └── main/
        ├── AndroidManifest.xml
        ├── java/com/synfusion/attendance/
        │   ├── DailyWorkApplication.kt
        │   ├── data/
        │   │   ├── entities/
        │   │   ├── daos/
        │   │   ├── AppDatabase.kt
        │   │   └── LocalAttendanceRepository.kt
        │   ├── ui/
        │   │   ├── components/
        │   │   ├── theme/
        │   │   ├── screens/ (cloned views)
        │   │   └── MainActivity.kt
        │   └── viewmodel/
        │       └── ViewModels & Factory
        └── res/ (drawable, mipmap, layout, values/strings.xml, values-hi/strings.xml)
```

---

## 7. Migration Strategy
Since this is a brand new, completely separate offline package/module, there is no legacy Firestore data to migrate. It uses a clean install with a fresh Room DB SQLite instance, providing 100% data integrity from day one.

---

## 8. Development Roadmap
- **Step 1:** Configure the isolated Android application project `android-offline-attendance-app`.
- **Step 2:** Write Room Database, Entities, DAOs, and the Repository.
- **Step 3:** Implement local Preferences DataStore.
- **Step 4:** Build local ViewModels and factory.
- **Step 5:** Clone pixel-perfect Compose UI screens.
- **Step 6:** Run compilation, tests, and pre-commit checks.
