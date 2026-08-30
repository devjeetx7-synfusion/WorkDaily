# WorkDaily — Daily Wage, Attendance & Passbook Ledger

[![Android CI Build](https://github.com/getsynfusion/workdaily/actions/workflows/offline-attendance-build.yml/badge.svg)](https://github.com/getsynfusion/workdaily/actions/workflows/offline-attendance-build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%201.5-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room%20SQLite%202.6-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

**WorkDaily** is a 100% offline, privacy-first Android application designed for daily wagers, contract workers, supervisors, and tradespeople across India to track daily wages, overtime credit, advance cash draws, and net financial balances in Indian Rupees (₹).

---

## Key Highlights

- 🔒 **100% Offline & Private:** Zero cloud sync, zero remote servers, zero ads, zero analytics/tracking SDKs. All data stays strictly on your device.
- ⚡ **Instant Settlement Engine:** Centralized financial engine (`AttendanceCalculationEngine`) automatically computes daily earnings, overtime pay, advance subtractions, and net balance (`YOU WILL RECEIVE`, `YOU NEED TO PAY BACK`, `SETTLED`).
- 📄 **A4 PDF Statement Export:** Local generation of styled A4 Passbook statements (monthly and all-time) ready for sharing via the Android system Sharesheet.
- 📦 **Local ZIP Backup & Restore:** Export and restore full encrypted/checksum-validated ZIP backups containing SQLite databases, manifests, and profile photos.
- 💬 **Hinglish Sarcastic Notifications:** Over 320 context-aware local notification templates written in casual Indian Hinglish with light roasting and sarcastic humor.
- 🌐 **Hindi & English Localization:** Full dual-language support with dynamic language switching, custom currency visual formatting, and localized calendar formats.

---

## Core Capabilities

1. **Daily Attendance Logging**
   - Record status as Present, Absent, Half Day, Holiday, or Advance-Only.
   - Override daily wage per entry or apply global default daily wage.
   - Add notes to attendance logs for job sites or task notes.

2. **Overtime & Advance Management**
   - Log overtime hours with standard multiplier options (1.0x, 1.5x, 2.0x, custom).
   - Track advance payouts and cash draws with clear balance adjustments.

3. **Passbook & Drill-Down Ledger**
   - Filter transactions by month or inspect all-time history.
   - Expand daily records in custom drill-down bottom sheets.

4. **Statistics & Interactive Chart**
   - Responsive multi-ring metrics for earnings, attendance breakdown, and advance total.
   - Interactive custom canvas `EarningsLineChart` with nearest-point touch detection and edge-aware tooltip cards.

---

## Application Architecture

WorkDaily is built using **Clean Architecture + MVVM** in Jetpack Compose:

```
UI (Compose) ──> ViewModel (StateFlow) ──> Calculation Engine / Repositories ──> Room SQLite / DataStore
```

For complete technical documentation, architecture details, and data relationship maps, consult [BRAIN.md](./BRAIN.md).

---

## Getting Started

### Prerequisites
- **Android Studio:** Hedgehog (2023.1.1) or newer
- **JDK:** Java 17
- **Android SDK:** minSdk 24 (Android 7.0), targetSdk 36

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/getsynfusion/workdaily.git
   cd workdaily/android-offline-attendance-app
   ```

2. Run Unit Tests:
   ```bash
   ./gradlew test
   ```

3. Build Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

4. Build Release APK:
   ```bash
   ./gradlew assembleRelease
   ```

---

## Technical Documentation

- 🧠 **[BRAIN.md](./BRAIN.md):** Architectural Master Knowledge Base for AI Coding Agents and Software Engineers.

---

## Developer Contact & Support

Built with ❤️ by **SynFusion**.
Official Support: `workdaily.synfusion@gmail.com`
