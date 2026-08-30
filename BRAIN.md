# WorkDaily Brain — Architectural Master Knowledge Base

> **Primary Source of Truth for AI Agents and Senior Developers**
> *Last Codebase Synchronization Audit: August 2026*
> *Repository Base: `android-offline-attendance-app`*

---

## 1. Project Identity

- **Application Name:** WorkDaily (Package ID: `com.synfusion.attendance`)
- **Developer / Organization:** SynFusion
- **Primary Domain:** Offline Daily Wage, Attendance, Advance, Overtime, and Settlement Ledger Application
- **Core Philosophy:** 100% Offline, Privacy-First, Zero Backend / Zero Cloud Dependency, Zero Ads / Trackers, Instant Local Calculation.
- **Target Platform:** Android (minSdk: 24 / Android 7.0 Nougat, targetSdk: 36 / Android 14/15/16).
- **Primary Target Audience:** Daily wagers, contract workers, tradespeople, shop staff, site workers, supervisors, and independent contractors in India tracking daily wages, advance payouts, overtime hours, and net balances in Indian Rupees (₹).

---

## 2. Current Project Scope

WorkDaily is a completely standalone, offline Android application written entirely in Kotlin using Jetpack Compose and Room SQLite database.

### What WorkDaily Does:
- **Daily Attendance Logging:** Track status as Present, Absent, Half Day, Holiday, or Advance-Only with custom daily wage overrides.
- **Overtime & Advance Management:** Log overtime hours with standard multiplier calculations (1.0x, 1.5x, 2.0x, custom) and advance cash draws.
- **Financial Reconciliation:** Centralized settlement engine calculating real-time net balances (`RECEIVE`, `PAY_BACK`, `SETTLED`).
- **Passbook Ledger & Drill-Down:** Monthly and All-Time Passbooks with detailed transaction histories, daily logs, and drill-down bottom sheets.
- **Passbook PDF Generation:** Local generation of styled PDF statements (monthly and all-time) with share/save functionality.
- **ZIP Backup & Restore Engine:** Version-aware 100% offline database export and import using ZIP archives containing SQLite DBs, JSON manifests, SHA-256 checksums, and profile avatar media files.
- **Hinglish Notification Engine:** Context-aware local daily reminders, weekly summaries, monthly salary logs, and milestone alerts using 320+ Hinglish sarcasm/funny templates.
- **Multi-Language Support:** Dynamic switching between English (`en`) and Hindi (`hi`) with localized UI strings, currency transformations, and date formatting.
- **Edge-to-Edge Adaptive UI:** Native Material 3 UI supporting phones, tablets, foldables, dark/light themes, and edge-to-edge display cutouts.

### What WorkDaily Does NOT Do:
- **No Remote Backend / APIs:** No REST endpoints, Firebase, WebSockets, or Cloud Sync.
- **No Ads / Analytics SDKs:** Zero tracking, no AdMob, no Firebase Analytics, no Crashlytics.
- **No Third-Party Cloud Backups:** Backup files are generated locally into user storage and handled via Android Sharesheet.

---

## 3. Technology Stack

| Layer | Technology | Version | Purpose / Usage |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `1.9.0` (Toolchain 17) | 100% Kotlin codebase with Coroutines & Flow |
| **UI Framework** | Jetpack Compose | `1.5.1` (BOM `2023.10.01`) | Declarative reactive UI components |
| **Design System** | Material 3 | `1.2.0` | Material You theme, dynamic colors, components |
| **Database** | Room SQLite | `2.6.1` (Schema v2) | Local ORM persistence for users and attendance |
| **Key-Value Storage**| DataStore Preferences | `1.0.0` | Application preferences (auth token, theme, lang) |
| **Code Generation** | KSP (Kotlin Symbol Processing)| `1.9.0-1.0.13` | Room DAO compilation and schema generation |
| **Async Programming**| Kotlin Coroutines & Flow | `1.7.3` | Asynchronous database and state flows |
| **Navigation** | Navigation Compose | `2.7.5` | Single-activity screen routing and transitions |
| **Background Jobs** | WorkManager | `2.8.1` | Periodic notification checks and fallback jobs |
| **Image Loading** | Coil Compose | `2.5.0` | Profile photo loading and caching |
| **PDF Generation** | Native Android `PdfDocument` | System Android API | Passbook statement rendering to PDF |
| **Serialization** | Google Gson | `2.10.1` | Backup JSON manifest and template parsing |
| **Build Tooling** | Gradle KTS | `8.1.1` AGP | Build configuration, minification, ProGuard |
| **Testing** | JUnit 4 | `4.13.2` | Core unit tests for math, PDF, and backup |

---

## 4. Application Architecture

WorkDaily strictly implements a **Clean Layered MVVM (Model-View-ViewModel)** architecture paired with the **Repository Pattern** and single-source-of-truth reactive state streams (`StateFlow` / `Flow`).

```
┌────────────────────────────────────────────────────────────────────────┐
│                               UI LAYER                                 │
│  MainActivity / Composables (Dashboard, Calendar, Passbook, Settings)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Collects StateFlow / Triggers Events
┌───────────────────────────────────▼────────────────────────────────────┐
│                             VIEWMODEL LAYER                            │
│ OfflineDashboardViewModel, OfflineCalendarViewModel, OfflinePassbookVM  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Calls Business Logic / Repositories
┌───────────────────────────────────▼────────────────────────────────────┐
│                    BUSINESS LOGIC / UTILITY ENGINES                    │
│  AttendanceCalculationEngine, NotificationRuleEngine, BackupManager    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Reads / Writes Persistent Data
┌───────────────────────────────────▼────────────────────────────────────┐
│                            REPOSITORY LAYER                            │
│ LocalAttendanceRepository, DrillDownRepository, OfflinePreferencesRepo │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Room DAOs / DataStore
┌───────────────────────────────────▼────────────────────────────────────┐
│                              DATA LAYER                                │
│          Room Database (SQLite)  │  DataStore Preferences              │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Guarantees:
1. **Unidirectional Data Flow (UDF):** User actions invoke ViewModel methods, which mutate repositories/database and emit updated `StateFlow` states back to UI.
2. **Synchronous Calculation Engine:** All financial calculations (`AttendanceCalculationEngine`) are centralized, deterministic, and isolated from UI or database state side-effects.
3. **Decoupled Backup Manager:** Backup/restore logic (`android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/backup/`) operates independently through isolated coordinator, manifest, checksum, and transaction classes.

---

## 5. Repository Structure

```
.
├── .github/
│   └── workflows/
│       └── offline-attendance-build.yml       # CI pipeline for debug & release APK builds
├── README.md                                  # Executive repository overview
├── BRAIN.md                                   # Master architectural knowledge base (THIS FILE)
└── android-offline-attendance-app/           # Primary Android Application Root
    ├── build.gradle.kts                       # Root Gradle project script
    ├── settings.gradle.kts                    # Module inclusion script
    ├── gradle.properties                      # JVM and build performance properties
    ├── gradlew / gradlew.bat                  # Gradle wrappers
    ├── keystore/                              # Release signing keystores
    │   ├── default-release.jks
    │   └── release.jks
    └── app/                                   # Application Module
        ├── build.gradle.kts                   # Module build, dependencies, KSP, ProGuard rules
        ├── proguard-rules.pro                 # Keep rules for Room, Gson, WorkManager
        ├── schemas/                           # Exported Room database schema definitions (v1, v2)
        └── src/
            ├── test/java/com/synfusion/attendance/ # Unit test suite
            │   ├── backup/BackupRestoreManagerTest.kt
            │   ├── utils/AttendanceCalculationEngineTest.kt
            │   ├── utils/ChartMathTest.kt
            │   ├── utils/FinancialReconciliationTest.kt
            │   ├── utils/NotificationSystemTest.kt
            │   └── utils/PdfExportTest.kt
            └── main/
                ├── AndroidManifest.xml        # Application manifest, permissions, receivers
                ├── res/                       # Android resource assets
                │   ├── drawable/              # Vector drawables and logo assets
                │   ├── mipmap-*/              # Adaptive launcher icons
                │   ├── raw/                   # notification_templates.json (320+ Hinglish templates)
                │   ├── values/                # Default English strings, colors, themes
                │   ├── values-hi/             # Hindi localized strings
                │   └── xml/file_paths.xml     # FileProvider paths for PDF and ZIP exports
                └── java/com/synfusion/attendance/ # Main Source Code
                    ├── DailyWorkApplication.kt # Application entry point & crash supervisor
                    ├── backup/                 # Modular ZIP backup & restore engine
                    │   ├── BackupLogger.kt
                    │   ├── BackupManager.kt
                    │   ├── BackupModels.kt
                    │   ├── BackupProvider.kt
                    │   ├── BackupRegistry.kt
                    │   ├── BackupValidator.kt
                    │   ├── ChecksumManager.kt
                    │   ├── DatabaseLifecycleCoordinator.kt
                    │   ├── JsonExporter.kt
                    │   ├── JsonImporter.kt
                    │   ├── ManifestManager.kt
                    │   ├── MediaExporter.kt
                    │   ├── MediaImporter.kt
                    │   ├── ProgressTracker.kt
                    │   ├── RestoreManager.kt
                    │   ├── TempDatabaseManager.kt
                    │   └── VersionMigrator.kt
                    ├── data/                   # Data layer & Room persistence
                    │   ├── AttendanceDatabase.kt
                    │   ├── DrillDownRepository.kt
                    │   ├── LocalAttendanceRepository.kt
                    │   ├── OfflinePreferencesRepository.kt
                    │   ├── daos/
                    │   │   ├── AttendanceDao.kt
                    │   │   ├── SummaryDao.kt
                    │   │   └── UserDao.kt
                    │   └── entities/
                    │       ├── AttendanceEntity.kt
                    │       ├── SummaryEntity.kt
                    │       └── UserEntity.kt
                    ├── ui/                     # Jetpack Compose UI screens & components
                    │   ├── AllTimePassbookScreen.kt
                    │   ├── CalendarScreen.kt
                    │   ├── CrashActivity.kt
                    │   ├── DashboardScreen.kt
                    │   ├── LanguageSelectionScreen.kt
                    │   ├── LanguageSelectorActivity.kt
                    │   ├── LoginScreen.kt
                    │   ├── MainActivity.kt
                    │   ├── PassbookScreen.kt
                    │   ├── SettingsScreen.kt
                    │   ├── SplashScreen.kt
                    │   ├── StatsScreen.kt
                    │   ├── components/
                    │   │   ├── BackupRestoreBottomSheet.kt
                    │   │   ├── CropScreen.kt
                    │   │   ├── CustomToggleTab.kt
                    │   │   ├── DrillDownBottomSheet.kt
                    │   │   ├── ExpandableNoteText.kt
                    │   │   ├── PdfExportLoadingDialog.kt
                    │   │   ├── SafeExternalLinkHandler.kt
                    │   │   ├── ShimmerPlaceholder.kt
                    │   │   └── SocialIcons.kt
                    │   ├── responsive/
                    │   │   └── WindowSize.kt
                    │   ├── screens/
                    │   │   └── LegalAndHelpScreens.kt
                    │   └── theme/
                    │       ├── Color.kt
                    │       ├── Theme.kt
                    │       └── Type.kt
                    ├── utils/                  # Domain business logic and calculation engines
                    │   ├── AllTimePassbookPdfModel.kt
                    │   ├── AmountFormatter.kt
                    │   ├── AttendanceCalculationEngine.kt
                    │   ├── BootReceiver.kt
                    │   ├── IndianCurrencyVisualTransformation.kt
                    │   ├── LifecycleUtils.kt
                    │   ├── LocaleHelper.kt
                    │   ├── LocalizationManager.kt
                    │   ├── LocalNotificationManager.kt
                    │   ├── NotificationAlarmReceiver.kt
                    │   ├── NotificationMessagePool.kt
                    │   ├── NotificationMessageSelector.kt
                    │   ├── NotificationRuleEngine.kt
                    │   ├── NotificationScheduler.kt
                    │   ├── NotificationTemplateRepository.kt
                    │   ├── NotificationWorker.kt
                    │   ├── OvertimeCalculator.kt
                    │   ├── PassbookPdfGenerator.kt
                    │   ├── PdfExportManager.kt
                    │   ├── PdfExportState.kt
                    │   └── [utils helpers]
                    └── viewmodel/              # State holders
                        ├── OfflineAllTimePassbookViewModel.kt
                        ├── OfflineAuthViewModel.kt
                        ├── OfflineCalendarViewModel.kt
                        ├── OfflineDashboardViewModel.kt
                        ├── OfflinePassbookViewModel.kt
                        ├── OfflineSettingsViewModel.kt
                        ├── OfflineStatsViewModel.kt
                        └── OfflineViewModelFactory.kt
```

---

## 6. Application Entry Point & Initialization Sequence

1. **`DailyWorkApplication.kt` (`onCreate`)**:
   - Configures global `Thread.setDefaultUncaughtExceptionHandler` redirecting fatal crashes to `CrashActivity.kt`.
   - Eagerly initializes `AttendanceDatabase.getInstance(this)` to run SQLite database creation or migration (e.g. v1 -> v2).
   - Loads notification templates via `NotificationTemplateRepository.loadTemplates(this)`.
   - Registers notification channels in `LocalNotificationManager.initChannels(this)`.
   - Schedules background notifications via `NotificationScheduler.scheduleAllSchedules(this)`.

2. **`MainActivity.kt` (`onCreate`)**:
   - Enables edge-to-edge rendering with cutout mode `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.
   - Reads user preference language synchronously from `OfflinePreferencesRepository` to set `AppCompatDelegate.setApplicationLocales` (preventing locale flash).
   - Checks `RestoreResultStore` for post-restore report metadata to display the `RestoreSuccessDetailDialog` if data was restored recently.
   - Evaluates system/user dark theme preference and wraps UI in `DailyWorkTheme` and `ProvideWindowSizeInfo`.

3. **Routing Evaluation (`OfflineDailyWorkApp`)**:
   - Observes `authTokenFlow`, `languageFlow`, and `splashEnabledFlow`.
   - Directs to `splash` -> `language_selection` (if first launch) -> `login` (if onboarding required) -> `dashboard`.

---

## 7. Feature Map

| Feature | Primary UI File | Primary Logic / Engine | Data / Persistence |
| :--- | :--- | :--- | :--- |
| **Attendance Logging** | `DashboardScreen.kt`, `CalendarScreen.kt` | `OfflineDashboardViewModel`, `OfflineCalendarViewModel` | `AttendanceEntity` via `AttendanceDao` |
| **Overtime Calculation** | `DashboardScreen.kt`, `CalendarScreen.kt` | `OvertimeCalculator.kt` | `AttendanceEntity.overtimeHours`, `overtimePay` |
| **Advance Draw Management** | `DashboardScreen.kt`, `CalendarScreen.kt` | `AttendanceCalculationEngine.kt` | `AttendanceEntity.advanceAmount` |
| **Passbook Ledger** | `PassbookScreen.kt`, `AllTimePassbookScreen.kt` | `OfflinePassbookViewModel`, `OfflineAllTimePassbookViewModel` | `AttendanceDao`, `SummaryDao` |
| **Settlement Calculation** | Passbook / Stats / PDF screens | `AttendanceCalculationEngine.calculateSettlement` | Evaluates Present, Half Day, Overtime vs Advance |
| **PDF Statement Export** | `PassbookScreen.kt`, `AllTimePassbookScreen.kt` | `PassbookPdfGenerator.kt`, `PdfExportManager.kt` | Generates system PDF file shared via FileProvider |
| **ZIP Backup & Restore** | `SettingsScreen.kt`, `BackupRestoreBottomSheet.kt` | `BackupManager.kt`, `RestoreManager.kt` | Exports/Imports SQLite DB, manifests, avatars |
| **Hinglish Notifications**| `LocalNotificationManager.kt` | `NotificationRuleEngine.kt`, `NotificationScheduler.kt` | Local AlarmManager & WorkManager |
| **Stats & Interactive Chart**| `StatsScreen.kt` | `AttendanceCalculationEngine.calculateStats` | `EarningsLineChart` custom canvas rendering |
| **Profile & Wage Settings**| `SettingsScreen.kt` | `OfflineSettingsViewModel.kt` | `UserEntity` in Room DB & DataStore |

---

## 8. Screen Map & Navigation Graph

```
[MainActivity]
  └── NavHost (Start: splash / language_selection / login / dashboard)
        ├── "splash" -> SplashScreen.kt
        ├── "language_selection" -> LanguageSelectionScreen.kt
        ├── "login" -> LoginScreen.kt (User setup / Daily wage input)
        └── "dashboard" -> DashboardScreen.kt
              └── BottomNavigationBar / AdaptiveNavigationRail (Nested Navigation)
                    ├── "home" -> Home Tab (Quick attendance, Today summary)
                    ├── "calendar" -> CalendarScreen.kt (Month calendar grid, daily logs)
                    ├── "passbook" -> PassbookScreen.kt (Monthly Passbook, PDF export)
                    ├── "stats" -> StatsScreen.kt (Earnings breakdown & interactive line chart)
                    └── "settings" -> SettingsScreen.kt (Profile, Wage, Backup/Restore, Theme, Legal)
                          ├── All-Time Passbook -> AllTimePassbookScreen.kt
                          ├── Crop Photo -> CropScreen.kt
                          └── Legal & Help -> LegalAndHelpScreens.kt
```

---

## 9. State Management Architecture

WorkDaily uses `ViewModel` instances combined with Kotlin `StateFlow` and Compose `collectAsStateWithLifecycle` for state observation.

- **`OfflineDashboardViewModel`**: Exposes `uiState: StateFlow<DashboardUiState>` containing current user, today's attendance record, current month summary, and loading states.
- **`OfflineCalendarViewModel`**: Exposes selected month/year, calendar days grid state, and daily attendance map (`Map<String, AttendanceEntity>`).
- **`OfflinePassbookViewModel`**: Exposes monthly calculation results (`FinancialSettlementResult`), daily line items, and PDF generation state (`PdfExportState`).
- **`OfflineStatsViewModel`**: Exposes `StatsUiState` including monthly vs all-time metrics, earnings list for line chart rendering, and category breakdowns.
- **`OfflineSettingsViewModel`**: Manages profile edits, daily wage adjustments, backup/restore progress states, and WhatsApp follow link actions.

---

## 10. Data Models & Database Schema (Room v2)

### Database: `AttendanceDatabase` (Schema Version 2)

#### 1. `users` Table (`UserEntity.kt`)
- `id`: `Int` (Primary Key, default 1)
- `name`: `String`
- `dailyWage`: `Double`
- `workType`: `String` (e.g. `DAILY`, `MONTHLY`, `PIECE_RATE`)
- `profileImageUri`: `String?`
- `createdAt`: `Long`
- `updatedAt`: `Long`

#### 2. `attendance` Table (`AttendanceEntity.kt`)
- `id`: `Long` (Auto-generate Primary Key)
- `userId`: `Int` (Foreign Key to `users.id`)
- `date`: `String` (ISO format `YYYY-MM-DD`, Indexed)
- `status`: `String` (`present`, `absent`, `half_day`, `holiday`, `advance_only`)
- `wageAmount`: `Double`
- `overtimeHours`: `Double`
- `overtimePay`: `Double`
- `advanceAmount`: `Double`
- `note`: `String?`
- `createdAt`: `Long`
- `updatedAt`: `Long`

#### 3. `summary` Table (`SummaryEntity.kt`)
- `id`: `Long` (Auto-generate Primary Key)
- `userId`: `Int`
- `month`: `String` (ISO format `YYYY-MM`)
- `totalEarnings`: `Double`
- `totalAdvance`: `Double`
- `netPayable`: `Double`
- `updatedAt`: `Long`

---

## 11. Centralized Financial Engine (`AttendanceCalculationEngine.kt`)

All monetary calculations in WorkDaily strictly execute through `AttendanceCalculationEngine` using exact `BigDecimal` arithmetic with `RoundingMode.HALF_UP` scaled to 2 decimal places.

### Core Calculation Rules:
1. **Daily Earnings**:
   $$\text{Daily Wage Earned} = \begin{cases} \text{dailyWage} & \text{if Present} \\ 0.5 \times \text{dailyWage} & \text{if Half Day} \\ 0 & \text{if Absent / Holiday / Advance-Only} \end{cases}$$
2. **Total Daily Credit**:
   $$\text{Total Credit} = \text{Daily Wage Earned} + \text{overtimePay}$$
3. **Financial Settlement Calculation**:
   $$\text{Net Balance} = \text{Total Credits} - \text{Total Advance Cash Drawn}$$
   - If $\text{Net Balance} > 0$: State = `RECEIVE` (User is owed money by employer).
   - If $\text{Net Balance} < 0$: State = `PAY_BACK` (User owes excess advance back).
   - If $\text{Net Balance} = 0$: State = `SETTLED`.

---

## 12. Backup & Restore Architecture (Modular ZIP Engine)

WorkDaily features a version-aware 100% local ZIP backup and restore engine located in `com.synfusion.attendance.backup`.

### Backup Flow (`BackupManager.kt`):
1. **Manifest Generation (`ManifestManager.kt`)**: Writes `manifest.json` with app version, schema version, record counts, and timestamp.
2. **Database Export (`JsonExporter.kt` / `TempDatabaseManager.kt`)**: Copies Room SQLite DB file safely after checkpointing WAL logs.
3. **Media Export (`MediaExporter.kt`)**: Copies profile avatar images from app internal storage.
4. **Checksum Calculation (`ChecksumManager.kt`)**: Computes SHA-256 signatures for every file in the payload.
5. **ZIP Compression (`BackupProvider.kt`)**: Bundles files into a compressed `.zip` archive and triggers standard Android Sharesheet via `FileProvider`.

### Restore Flow (`RestoreManager.kt`):
1. **Archive Inspection (`BackupValidator.kt`)**: Unpacks archive in temporary directory and checks SHA-256 checksums.
2. **Version Migration (`VersionMigrator.kt`)**: Evaluates database schema compatibility and migrates legacy structures if necessary.
3. **Lifecycle Coordination (`DatabaseLifecycleCoordinator.kt`)**: Closes active Room database connection, replaces DB file, re-opens Room instance, and clears cached singletons.

---

## 13. Hinglish Local Notification Subsystem

Notifications are handled locally via `AlarmManager` exact alarms and `WorkManager` fallback jobs.

- **Templates Repository (`NotificationTemplateRepository.kt`)**: Loads 320+ sarcastic, funny, light-roasting Hinglish templates from `res/raw/notification_templates.json`.
- **Rule Engine (`NotificationRuleEngine.kt`)**: Evaluates trigger criteria (daily reminder at 8:00 PM, weekly summary on Sunday, monthly report on 1st of month) and replaces dynamic placeholders (`{userName}`, `{todayWage}`, `{totalEarnings}`, `{netPayable}`, `{streak}`).
- **Cooldown History**: Prevents immediate repetition of notification templates using SharedPreferences category cooldown tracking.

---

## 14. PDF Generation Subsystem (`PassbookPdfGenerator.kt`)

WorkDaily creates styled A4 PDF documents directly on-device using Android's native `android.graphics.pdf.PdfDocument`.

- **Layout & Rendering**: Draws header branding, user profile box, financial summary cards, multi-column transaction tables, and disclaimer footers.
- **Multi-page Support**: Automatically calculates table row heights and paginates transactions clean across page boundaries.
- **Empty State Protection**: `PdfExportManager.kt` checks `records.isEmpty()` before rendering to avoid creating blank PDFs and returns a localized error if no records exist.

---

## 15. Testing Suite

The repository includes unit tests located in `android-offline-attendance-app/app/src/test/java/com/synfusion/attendance/`:

- **`AttendanceCalculationEngineTest.kt`**: Validates wage, overtime, advance, and settlement calculation precision.
- **`FinancialReconciliationTest.kt`**: Tests edge cases for negative balances, zero days, and mixed status calculations.
- **`BackupRestoreManagerTest.kt`**: Tests backup manifest generation, SHA-256 checksum validation, and archive integrity.
- **`PdfExportTest.kt`**: Tests PDF model mapping and string formatting logic.
- **`ChartMathTest.kt`**: Validates line chart Y-axis scale generation (`calculateNiceScale`).
- **`NotificationSystemTest.kt`**: Verifies placeholder substitution and template selection.

Run all tests via:
```bash
cd android-offline-attendance-app
./gradlew test
```

---

## 16. High-Risk Code Areas & Safety Constraints

| Area | Location | Why High Risk | Safety Rule |
| :--- | :--- | :--- | :--- |
| **Calculation Engine** | `AttendanceCalculationEngine.kt` | Changing math here affects Passbook, Stats, Home, and PDF simultaneously. | Always run `./gradlew test` after modifying any financial formula. |
| **Database Schema** | `AttendanceDatabase.kt`, Entities | Schema alterations require Room migration script and schema export update. | Increment schema version, update `schemas/`, and add migration path in `AttendanceDatabase`. |
| **Database Lifecycle** | `DatabaseLifecycleCoordinator.kt` | Improper DB closure during restore causes SQLite database locking or corruption. | Ensure `AttendanceDatabase.closeInstance()` is called prior to file replacement. |
| **Date Range Formatting** | `DrillDownRepository.kt` | Date range boundary mismatches cause missing or double-counted attendance days. | Maintain strict start-inclusive (`startDate <= date`) and end-exclusive (`date < endDate`) ISO ranges. |

---

## 17. Safe Modification Rules for AI Agents

1. **Always Read First:** Inspect existing implementation in source files before introducing new models or methods.
2. **Preserve Offline Architecture:** Never introduce external HTTP libraries, remote APIs, or cloud sync dependencies.
3. **Keep Math Centralized:** Never calculate earnings or net balances inside Composable UI functions; always call `AttendanceCalculationEngine`.
4. **Enforce Test Verification:** Execute `./gradlew test` inside `android-offline-attendance-app` after any modifications to ensure zero regression.
5. **Maintain Localized Hinglish Wording:** Keep user-facing notification text in casual Indian Hinglish with warm, light-hearted tone.

---

## 18. Document Maintenance Rule

When core features, database schemas, or utility architectures in `android-offline-attendance-app` are modified, update `BRAIN.md` immediately to ensure continuous AI-agent alignment with the codebase.
