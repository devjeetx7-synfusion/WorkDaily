# WorkDaily User Brain — Architectural Master Knowledge Base

> **Primary Source of Truth for AI Agents and Senior Developers**
> *Last Codebase Synchronization Audit: August 2026*
> *Repository Base: `android-offline-attendance-app`*

---

## 1. Project Identity

- **Application Name:** WorkDaily User (Package ID: `com.synfusion.attendance`)
- **Developer / Organization:** SynFusion
- **Primary Domain:** Offline Daily Wage, Attendance, Advance, Overtime, and Settlement Ledger Application for Individual Users
- **Core Philosophy:** 100% Offline, Privacy-First, Zero Backend / Zero Cloud Dependency, Zero Ads / Trackers, Instant Local Calculation.
- **Target Platform:** Android (minSdk: 24 / Android 7.0 Nougat, targetSdk: 36 / Android 14/15/16).
- **Primary Target Audience:** Daily wagers, contract workers, tradespeople, shop staff, site workers, supervisors, and independent contractors in India tracking daily wages, advance payouts, overtime hours, and net balances in Indian Rupees (₹).

---

## 2. Current Project Scope

WorkDaily User is a completely standalone, offline Android application written entirely in Kotlin using Jetpack Compose and Room SQLite database.

### What WorkDaily User Does:
- **Daily Attendance Logging:** Track status as Present, Absent, Half Day, Holiday, or Advance-Only with custom daily wage overrides.
- **Overtime & Advance Management:** Log overtime hours with standard multiplier calculations (1.0x, 1.5x, 2.0x, custom) and advance cash draws.
- **Financial Reconciliation:** Centralized settlement engine calculating real-time net balances (`RECEIVE`, `PAY_BACK`, `SETTLED`).
- **Passbook Ledger & Drill-Down:** Monthly and All-Time Passbooks with detailed transaction histories, daily logs, and drill-down bottom sheets.
- **Passbook PDF Generation:** Local generation of styled PDF statements (monthly and all-time) with share/save functionality.
- **ZIP Backup & Restore Engine:** Version-aware 100% offline database export and import using ZIP archives containing SQLite DBs, JSON manifests, SHA-256 checksums, and profile avatar media files.
- **Hinglish Notification Engine:** Context-aware local daily reminders, weekly summaries, monthly salary logs, and milestone alerts using 320+ Hinglish sarcasm/funny templates.
- **Multi-Language Support:** Dynamic switching between English (`en`) and Hindi (`hi`) with localized UI strings, currency transformations, and date formatting.
- **Edge-to-Edge Adaptive UI:** Native Material 3 UI supporting phones, tablets, foldables, dark/light themes, and edge-to-edge display cutouts.

### What WorkDaily User Does NOT Do:
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

WorkDaily User strictly implements a **Clean Layered MVVM (Model-View-ViewModel)** architecture paired with the **Repository Pattern** and single-source-of-truth reactive state streams (`StateFlow` / `Flow`).

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
3. **Guaranteed Swipe-to-Refresh Lifecycle:** ViewModels implement `RefreshableViewModel`. Background refresh tasks (`refresh()`, `loadLocalData()`) use strict `try-catch-finally` state updates to guarantee `isRefreshing = false` is always emitted upon completion, error, or job cancellation, preventing stuck refresh indicators.
4. **Decoupled Backup Manager:** Backup/restore logic (`android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/backup/`) operates independently through isolated coordinator, manifest, checksum, and transaction classes.

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

## 6. Code-Level Detail for Key Components

To help future AI agents navigate and modify the codebase without full blind repository scans, this section details key classes, engines, models, and UI components.

### 6.1 Calculation & Domain Engines

#### `AttendanceCalculationEngine.kt`
- **What it does:** Master math engine performing all financial calculations across Home, Passbook, All-Time Passbook, Statistics, and PDF statements. Uses `BigDecimal` arithmetic with `RoundingMode.HALF_UP` scaled to 2 decimal places.
- **Why it exists:** Guarantees uniform calculation rules, eliminating rounding discrepancies or divergent math across screens.
- **Where it is used:** `OfflineDashboardViewModel`, `OfflineCalendarViewModel`, `OfflinePassbookViewModel`, `OfflineStatsViewModel`, `OfflineAllTimePassbookViewModel`, `PassbookPdfGenerator`.
- **Data read/written:** Accepts lists of `AttendanceEntity` and daily wage defaults; produces `FinancialSettlementResult`, passbook log summaries, and daily chart series.
- **Potential Impact of Modifying:** Modifying formulas or rounding modes here directly alters all displayed financial figures and PDF statement totals across the entire application.

#### `OvertimeCalculator.kt`
- **What it does:** Parses overtime note markers (e.g. `[OT_WAGE_500]`) and calculates custom overtime amounts.
- **Why it exists:** Supports both hourly multiplier overtime and explicit lump-sum overtime entries.
- **Where it is used:** `DashboardScreen`, `CalendarScreen`, `AttendanceCalculationEngine`.

#### `AmountFormatter.kt`
- **What it does:** Formats monetary values for display in Indian Rupees (e.g. `₹1,250.00` or `1,250`).
- **Why it exists:** Enforces clean, standard currency formatting throughout the app.

---

### 6.2 Data Layer & Persistence

#### `AttendanceDatabase.kt`
- **What it does:** Room database singleton (v2 schema) providing access to `UserDao`, `AttendanceDao`, and `SummaryDao`.
- **Why it exists:** Primary local ORM persistence layer.
- **Where it is used:** Repositories, backup coordinators, application initialization.
- **Potential Impact:** Schema modifications require adding migration scripts and updating exported schema files in `schemas/`.

#### `LocalAttendanceRepository.kt`
- **What it does:** High-level data accessor bridging DAOs with ViewModels. Reads and writes `UserEntity` and `AttendanceEntity`.
- **Where it is used:** `OfflineDashboardViewModel`, `OfflineCalendarViewModel`, `OfflinePassbookViewModel`, `OfflineStatsViewModel`, `PdfExportManager`.

#### `OfflinePreferencesRepository.kt`
- **What it does:** Wraps DataStore Preferences to manage auth tokens, theme preferences, language selections, and profile photo paths.

---

### 6.3 Swipe-to-Refresh Architecture

All main tab screens support user pull-to-refresh gestures. The architecture operates as follows:

1. **State Flow:** ViewModels expose `isRefreshing: Boolean` as part of their main `StateFlow` state data class.
2. **ViewModel Implementation:**
   - ViewModel methods (`refresh()`, `loadLocalData()`) set `isRefreshing = true` on start.
   - All background coroutine operations execute within `try-catch-finally` blocks where `isRefreshing = false` is guaranteed in `finally`.
   - Minimum execution delays (~400ms) prevent visual indicator flicker during fast local database reads.
3. **UI Integration (`PullToRefreshContainer`):**
   - Screens (`DashboardScreen`, `CalendarScreen`, `StatsScreen`, `PassbookScreen`) create `rememberPullToRefreshState()`.
   - `LaunchedEffect(pullRefreshState.isRefreshing)` invokes `viewModel.refresh()`.
   - `LaunchedEffect(state.isRefreshing)` calls `pullRefreshState.endRefresh()` whenever `!state.isRefreshing`.
   - Root layout `Box` elements attach `.nestedScroll(pullRefreshState.nestedScrollConnection)` around the entire content hierarchy (including error and shimmer states) to ensure swipe gestures trigger reliably regardless of page state.

---

### 6.4 PDF & Backup Engines

#### `PassbookPdfGenerator.kt` & `PdfExportManager.kt`
- **What it does:** `PassbookPdfGenerator` renders A4 PDF pages using native `android.graphics.pdf.PdfDocument`. `PdfExportManager` validates record presence (`records.isEmpty()`), handles background generation, and shares files via `FileProvider`.
- **Where it is used:** `PassbookScreen`, `AllTimePassbookScreen`.

#### `BackupManager.kt` & `RestoreManager.kt`
- **What it does:** `BackupManager` packages SQLite database files, profile images, and JSON manifests into SHA-256 validated ZIP archives. `RestoreManager` validates checksums, closes active DB instances (`DatabaseLifecycleCoordinator`), replaces local database files, and triggers UI refresh.

---

## 7. High-Risk Code Areas & Safety Constraints

| Area | Location | Why High Risk | Safety Rule |
| :--- | :--- | :--- | :--- |
| **Calculation Engine** | `AttendanceCalculationEngine.kt` | Changing math here affects Passbook, Stats, Home, and PDF simultaneously. | Always run `./gradlew test` after modifying any financial formula. |
| **Database Schema** | `AttendanceDatabase.kt`, Entities | Schema alterations require Room migration script and schema export update. | Increment schema version, update `schemas/`, and add migration path in `AttendanceDatabase`. |
| **Database Lifecycle** | `DatabaseLifecycleCoordinator.kt` | Improper DB closure during restore causes SQLite database locking or corruption. | Ensure `AttendanceDatabase.closeInstance()` is called prior to file replacement. |
| **Date Range Formatting** | `DrillDownRepository.kt` | Date range boundary mismatches cause missing or double-counted attendance days. | Maintain strict start-inclusive (`startDate <= date`) and end-exclusive (`date < endDate`) ISO ranges. |

---

## 8. Safe Modification Rules for AI Agents

1. **Always Read First:** Inspect existing implementation in source files before introducing new models or methods.
2. **Preserve Offline Architecture:** Never introduce external HTTP libraries, remote APIs, or cloud sync dependencies.
3. **Keep Math Centralized:** Never calculate earnings or net balances inside Composable UI functions; always call `AttendanceCalculationEngine`.
4. **Maintain Refresh State Safety:** Ensure every background refresh job in ViewModels uses `finally` to set `isRefreshing = false`.
5. **Enforce Test Verification:** Execute `./gradlew test` inside `android-offline-attendance-app` after any modifications to ensure zero regression.
6. **Maintain Localized Hinglish Wording:** Keep user-facing notification text in casual Indian Hinglish with warm, light-hearted tone.

---

## 9. Document Maintenance Rule

When core features, database schemas, or utility architectures in `android-offline-attendance-app` are modified, update `BRAIN.md` immediately to ensure continuous AI-agent alignment with the codebase.
