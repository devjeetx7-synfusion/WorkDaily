# WorkDaily 🛠️

**WorkDaily** is a professional, multi-platform workforce management solution designed for **Contractors** and **Individual Professionals**. It streamlines daily attendance tracking, financial management, and work history logging through a seamless experience on both Android and Web.

---

## 📸 Screenshots

*(Placeholder image paths - update with actual project screenshots)*

| Login & Onboarding | Contractor Dashboard | Personal Dashboard |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Login+Screen" alt="Login" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Contractor+View" alt="Contractor" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Personal+View" alt="Personal" width="200" /> |

| Worker History | PDF Reports | Admin App |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=History+Logs" alt="History" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=PDF+Export" alt="Reports" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Admin+Panel" alt="Admin" width="200" /> |

---

## 🚀 Key Features

### 🏗️ Contractor Management
Built for professionals managing teams of workers, helpers, and mistris.
- **Worker Database:** Add and manage a complete list of laborers with specific work types.
- **Bulk Attendance:** Mark attendance (Present, Absent, Half-day) for the entire team in seconds.
- **Payment Tracking:** Log advances, partial payments, and full settlements.
- **Due & Overtime Management:** Keep track of pending dues and overtime hours worked by each staff member.

### 👤 Personal Mode
Tailored for individual workers and self-employed professionals.
- **Daily Work Log:** Record your own attendance, overtime, and work notes.
- **Personal Passbook:** A digital ledger to track your earnings, advances received, and remaining balances.
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
