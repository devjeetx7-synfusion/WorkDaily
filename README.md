# WorkDaily App

> A 100% offline, privacy-first Android application to track daily wages, attendance, overtime credit, advance payments, and financial passbook balances.

---

## ✨ What is WorkDaily App?

**WorkDaily App** is designed for daily wagers, contract workers, tradespeople, supervisors, and independent workers across India. It provides a simple, reliable way to record daily attendance, log advance cash draws, calculate overtime pay, and keep a clean digital passbook ledger—completely offline on your phone.

---

## 🚀 Features

- 🔒 **100% Offline & Private** — All data stays strictly on your phone. No cloud sync, no servers, no ads, and no tracking.
- ⚡ **Instant Settlement Engine** — Real-time calculation of daily earnings, overtime, advance deductions, and net pending balances (`YOU WILL RECEIVE`, `YOU NEED TO PAY BACK`, or `SETTLED`).
- 📄 **A4 Passbook PDF Export** — Generate clean monthly and all-time financial passbook statements ready for printing or sharing via WhatsApp.
- 📦 **Local ZIP Backup & Restore** — Export and restore full encrypted backups (SQLite database, settings, and profile photo) with SHA-256 checksum protection.
- 💬 **Hinglish Daily Reminders** — 320+ friendly, sarcastic, and funny local notifications written in everyday Indian Hinglish.
- 🌐 **Hindi & English Support** — Full dual-language interface with custom currency formatting (₹) and localized calendar views.

---

## 📱 What You Can Do

1. **Log Attendance Easily** — Mark Full Day, Half Day, Absent, Holiday, or Advance-Only with optional custom wage overrides and notes.
2. **Track Overtime & Advance Cash** — Log overtime hours and record advance payouts with instant balance updates.
3. **Inspect Monthly & All-Time Passbooks** — Review transaction histories with drill-down views for earnings, attendance, and advances.
4. **View Interactive Earnings Charts** — Track daily income trends over time with touch-interactive chart tooltips.

---

## 🔒 Offline & Privacy First

WorkDaily App operates without an account or backend cloud server. All your data—profile info, daily wages, attendance records, and notes—is saved locally on your device inside Room SQLite database and DataStore preferences. Uninstalling the application deletes internal local database records, so creating periodic backup ZIP files is recommended.

---

## 🧩 Built With

- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room SQLite (Schema v2)
- **Key-Value Storage:** DataStore Preferences
- **Async Processing:** Kotlin Coroutines & Flow
- **Background Tasks:** WorkManager & AlarmManager
- **PDF Engine:** Native Android `PdfDocument` API

---

## 📂 Project Structure

```
workdaily/
├── BRAIN.md                            # Architectural Master Knowledge Base for AI & Developers
├── README.md                           # Project Overview
└── android-offline-attendance-app/     # Primary Android Application Module
    ├── build.gradle.kts
    └── app/src/main/java/com/synfusion/attendance/
        ├── backup/                     # Modular ZIP backup & restore engine
        ├── data/                       # Room database entities, DAOs, repositories
        ├── ui/                         # Jetpack Compose UI screens, components, responsive layouts
        ├── utils/                      # AttendanceCalculationEngine, PDF generator, notifications
        └── viewmodel/                  # MVVM ViewModels and state management
```

---

## 📖 Documentation

For detailed technical documentation, calculation engine rules, database schemas, backup flows, and architecture details, refer to [BRAIN.md](./BRAIN.md).

---

## 🛠️ Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK (minSdk 24, targetSdk 36)

### Build Commands

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

4. Build Release APK:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 📌 Project Status

Active production app. Built and maintained by **SynFusion**.
Support Email: `workdaily.synfusion@gmail.com`
