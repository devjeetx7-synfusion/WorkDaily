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
- **Advanced Analytics:** In-depth insights into labor costs and earning trends.
- **Enhanced Export:** Granular control over report generation and formats.
- **Premium Insights:** Comparative data and productivity tracking.
- **Ad-Free Experience:** A clean, focused interface for power users.

### 🛡️ Admin Panel (Android)
- **User Management:** Monitor, block, or modify user roles.
- **Premium Control:** Manage subscriptions and activation statuses.
- **App Configuration:** Toggle features like splash screens or maintenance modes remotely.
- **Broadcast Notifications:** Send real-time updates via Firebase Cloud Messaging.

---

## 🛠️ Tech Stack

### Android (User & Admin)
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **DI:** Manual Injection (User) / Hilt (Admin)
- **Local DB:** Room
- **Networking:** Firebase SDK
- **Image Loading:** Coil
- **Billing:** Google Play Billing

### Web App
- **Framework:** React 18
- **Language:** TypeScript
- **Bundler:** Vite
- **Styling:** Tailwind CSS
- **Animations:** Framer Motion

### Backend
- **Database:** Firebase Firestore (NoSQL)
- **Auth:** Firebase Authentication (Google Sign-In)
- **Serverless:** Firebase Cloud Functions
- **Security:** Firebase App Check

---

## 🏗️ Architecture

**WorkDaily** uses a sophisticated, role-based nested Firestore structure to ensure strict data isolation and high performance:

- `users/{userId}`: Root user document.
  - `contractor/data/`: Isolated container for contractor-specific entities.
    - `workers/{workerId}/attendance/{date}`: Nested worker history.
  - `personal/data/`: Isolated container for individual tracking.
    - `attendance/{date}`: Daily logs and overtime data.

This architecture ensures that users only ever interact with their own data, reducing query complexity and improving security.

---

## 📂 Folder Structure

- `/android-app`: The primary native Android application for users.
- `/android-admin-app`: The native Android management tool for administrators.
- `/src`: The React-based web application source code.
- `/functions`: Firebase Cloud Functions for backend logic.
- `/docs`: Project documentation and refactor guides.
- `/.github`: CI/CD workflows for automated builds and deployment.

---

## 🛡️ Security

- **Firebase Rules:** Granular Firestore rules enforce user-level data isolation.
- **App Check:** Protects backend resources from unauthorized traffic.
- **Role-Based Access:** Admin-only collections (e.g., `admin_logs`) are protected by server-side checks.
- **Secure Signing:** Automated GitHub Actions workflows use repository secrets for keystore management.

---

## ⚙️ Installation Guide

### Android App
1. Clone the repository.
2. Add your `google-services.json` to the `app/` directory.
3. Sync Gradle in Android Studio.
4. Build and run on a physical device or emulator.

### Web App
1. Navigate to the root directory.
2. Run `npm install --legacy-peer-deps`.
3. Create a `.env` file based on `.env.example`.
4. Run `npm run dev` to start the local server.

---

## 🗺️ Roadmap
- [ ] Play Store official release.
- [ ] Enhanced offline synchronization.
- [ ] Multi-language localization expansion.
- [ ] AI-driven labor cost predictions.
- [ ] Advanced CSV/Excel report exports.

---

## 🤝 Contribution
Contributions are welcome! Please follow these steps:
1. Fork the project.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes.
4. Push to the branch.
5. Open a Pull Request.

---

## 📝 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

> **GitHub Description:** Modern attendance & workforce management ecosystem with Android, Web & Admin Panel support.

> **Topics:** `attendance-management`, `worker-management`, `kotlin`, `jetpack-compose`, `firebase`, `reactjs`, `vite`, `attendance-app`, `contractor-management`, `android`, `work-management`, `pdf-generation`, `premium-system`
