# WorkDaily 🛠️

<div align="center">
  <img src="https://via.placeholder.com/150" alt="WorkDaily Logo" width="120" />
  <h1>WorkDaily</h1>
  <p><strong>Smart Attendance & Worker Management Ecosystem for Contractors and Individuals.</strong></p>

  ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
  ![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
  ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)
  ![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
  ![MIT License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
</div>

---

## 📸 Screenshots

| Login & Onboarding | Dashboard | Contractor Panel |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Login+Screen" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Dashboard" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Contractor+Panel" width="200" /> |

| Passbook | Premium Screen | Admin App |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Passbook" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Premium+Screen" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Admin+App" width="200" /> |

---

## 🚀 About Project

**WorkDaily** is a modern, full-stack attendance and workforce management ecosystem. It provides a robust set of tools for both individual professionals tracking their own work and contractors managing diverse teams of laborers.

The ecosystem consists of:
- **Android User App:** The primary interface for tracking attendance and finances.
- **Android Admin App:** A dedicated tool for system administrators to manage the user base and platform.
- **Web Dashboard:** A high-performance web interface matching the mobile experience.

---

## ✨ Main Features

### 👤 Personal Mode
Tailored for individual tracking and self-employed professionals.
- **Daily Attendance:** Log your presence with one tap.
- **Overtime Tracking:** Record extra hours and notes for every workday.
- **Financial Ledger:** Track advances received and pending dues.
- **Personal Passbook:** View a detailed history of your work and earnings.
- **Monthly Summaries:** Automated aggregation of work days and total payouts.

### 🏗️ Contractor Mode
Professional workforce management for teams (Workers, Helpers, Mistris).
- **Worker Management:** Add and organize workers by specific work types.
- **Batch Attendance:** Mark attendance for your entire team simultaneously.
- **Payment Tracking:** Log full payments, half payments, and advances per worker.
- **PDF Reports:** Generate and share professional worker passbooks as PDF documents.
- **Work Type Management:** Customize roles and wage structures for your team.

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
