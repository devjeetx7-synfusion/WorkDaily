# Firestore Architecture Refactor Guide

This document outlines the architectural patterns and best practices implemented during the Firestore nested structure refactor for the **WorkDaily** ecosystem.

## 🏗️ New Nested Structure

The application has transitioned from a flat collection structure to a strictly isolated, role-based nested hierarchy under each user document. This ensures data privacy and simplifies security rules.

- `users/{userId}`
  - `contractor/data/`
    - `workers/{workerId}`
      - `attendance/{date}`
    - `workTypes/{typeId}`
    - `summaries/{YYYY-MM}`
  - `personal/data/`
    - `attendance/{date}`
    - `summaries/{YYYY-MM}`

## ✅ Key Benefits

- **Strict Data Isolation:** Users have exclusive access to their own sub-collections, preventing unauthorized data exposure.
- **Improved Performance:** Eliminates the need for complex `where` filters (e.g., `contractorId`) for basic ownership checks.
- **Scalability:** Data is naturally partitioned by user, supporting large-scale usage without performance degradation.
- **Summary Optimization:** Aggregated statistics are pre-calculated in the `summaries` collection, significantly reducing read costs and enhancing UI responsiveness.

## 🛠️ Implementation Details

### 1. Centralized Repository (`FirestoreRepository.kt`)
All Firestore interactions must be routed through the `FirestoreRepository`. Key responsibilities include:
- Resolving correct document paths for nested collections.
- Managing automatic cascade deletions for worker-related records.
- Handling monthly summary updates atomically during attendance operations.

### 2. ViewModel Architecture
ViewModels receive the `FirestoreRepository` via a `ViewModelFactory`. Standard practices include:
- Utilizing path-based document access for precision.
- Prioritizing `summaries` for high-level dashboard statistics.
- Implementing optimistic UI updates to provide a responsive user experience.

### 3. Data Models
Redundant identification fields (e.g., `contractorId`, `user_id`) have been removed from data classes. Document paths now serve as the primary source of truth for ownership and relational context.

## ⚠️ Edge Case Handling

### 1. Partial Migration
The `DataMigrationManager` handles the transition of legacy data. If a migration is interrupted:
- The process is idempotent (using `set` operations), allowing for safe re-runs.
- Legacy collections are retained until full verification of the new structure is confirmed.

### 2. Data Integrity
- Implement `snapshot.exists()` checks in all listeners.
- Utilize default values (e.g., standard wage rates) when specific fields are absent in the database.

## 🚀 Compose UI Best Practices

- Observe data changes using `StateFlow` from ViewModels.
- Manage UI updates within `LaunchedEffect` or `collectAsStateWithLifecycle` for lifecycle-aware execution.
- Provide comprehensive loading, empty, and error states for all nested data operations.
- Utilize `isRefreshing` flags to provide standard pull-to-refresh functionality.

---
*Maintained by Jules - Software Engineer*- **Personal Passbook:** A digital ledger to track your earnings, advances received, and remaining balances.
- **History Tracking:** Detailed month-by-month summary of work days and payments.

### 💎 Premium Features
*Available via subscription models (Monthly, Yearly, etc.)*
- **Enhanced Insights:** Deep dive into labor costs and earning trends.
- **Advanced Exporting:** More granular control over report generation.
- **Ad-Free Experience:** Focus on your work without distractions.

### 📱 Integrated Experience
- **PDF Generation:** Instantly generate professional PDF passbooks and work reports for sharing via WhatsApp or Email.
- **Admin Panel:** A dedicated Android app for system-wide user management, notification broadcasting, and configuration control.
- **Theming & Localization:** Supports multiple languages (including Hindi) and customizable UI preferences like splash screen toggles.

---

## 🛠️ Tech Stack

**WorkDaily** utilizes a modern, robust tech stack to ensure performance and reliability.

- **Android App:** Native Kotlin, Jetpack Compose (Material 3), Hilt (Admin App), Room (Local Cache), Coil.
- **Web App:** React 18, TypeScript, Vite, Tailwind CSS, Framer Motion.
- **Backend:** Firebase Firestore (NoSQL), Firebase Authentication (Google Sign-In), Firebase Cloud Functions.
- **In-App Purchases:** Google Play Billing integration.
- **CI/CD:** GitHub Actions for automated Android builds (APK/AAB) and Web deployment.

---

## 📋 How It Works

1.  **Authentication:** Securely sign in using Google Auth.
2.  **Role Selection:** Choose between **Contractor** or **Personal** mode based on your needs.
3.  **Setup:**
    - *Contractors:* Add your workers and define work types.
    - *Individuals:* Set your base daily wage and preferences.
4.  **Daily Tracking:** Log attendance and any financial transactions (advances/dues).
5.  **Review & Export:** Monitor stats through the dashboard and generate PDF reports for record-keeping or sharing.

---

## ⚙️ Setup & Installation

### Android Apps (`/android-app` & `/android-admin-app`)
1.  **Clone the Repo:** `git clone <repository-url>`
2.  **Open in Android Studio:** Open the desired app directory.
3.  **Firebase Setup:**
    - Create a project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android App with package name `com.dailywork.attedance` (User) or `com.dailywork.admin` (Admin).
    - Download `google-services.json` and place it in the `app/` directory of the respective project.
4.  **Build:** Sync Gradle and run the `debug` or `release` variant.

### Web Application (`/src`)
1.  **Install Dependencies:** `npm install --legacy-peer-deps`
2.  **Environment Variables:** Create a `.env` file with your Firebase configuration (see `.env.example`).
3.  **Run Development Server:** `npm run dev`

---

## 🛡️ Security & Configuration

- **Environment Secrets:** Never commit your production `.env` or `google-services.json` files.
- **Firebase Rules:** Ensure the provided `firestore.rules` are deployed to protect user data isolation.
- **Signing:** The Android app uses a repository-stored keystore for builds. Ensure `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` are configured in your CI environment for release builds.

---

## 📝 License & Author

Copyright © 2024 **WorkDaily**. Licensed under the MIT License.
Crafted with ❤️ by **Dharmjeet**.
