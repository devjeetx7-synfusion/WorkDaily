# DailyWork 🛠️

**DailyWork** is a comprehensive, dual-mode web application designed to help **Contractors** and **Individuals** manage their daily labor, attendance, earnings, and payments efficiently.

Designed with a mobile-first approach, it offers a seamless experience akin to native apps, bringing professional workforce management right to your browser.

Crafted by **Dharmjeet**.

---

## 📸 Screenshots

*(Replace the placeholder links below with actual screenshots of your application)*

| Login Screen | Dashboard (Contractor) | Dashboard (Personal) |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Login+Screen" alt="Login Screen" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Contractor+Dashboard" alt="Contractor Dashboard" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Personal+Dashboard" alt="Personal Dashboard" width="200" /> |

| Worker Details | Daily Attendance | Settings |
| :---: | :---: | :---: |
| <img src="https://via.placeholder.com/250x500.png?text=Worker+Details" alt="Worker Details" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Daily+Attendance" alt="Daily Attendance" width="200" /> | <img src="https://via.placeholder.com/250x500.png?text=Settings" alt="Settings" width="200" /> |

---

## 🚀 Key Features

### 🏗️ Contractor Mode
A full-fledged worker management system for contractors handling multiple laborers.
- **Worker Management:** Add, edit, and assign workers to dynamic "Custom Work Types" (e.g., Electrician, Mistri, Painter).
- **Batch Attendance:** Quickly log Present, Absent, or Half Days for all active workers via an intuitive Contractor Calendar.
- **Financial Passbook:** Track gross earnings and advance payments per worker. Easily view "Net Payable" balances at a glance.
- **Global Logs & Stats:** Access comprehensive statistics showing total man-days, aggregated labor costs, and detailed historical logs across the entire workforce.
- **PDF & WhatsApp Integration:** Instantly generate professional "Worker Passbooks" as PDF receipts or share them directly via a dynamically formatted WhatsApp message.

### 👤 Personal Mode
A dedicated dashboard tailored for tracking your own self-employed daily wages.
- **Daily Check-ins:** Mark your own attendance directly from the dashboard, tracking overtime hours, specific absence reasons, and daily notes.
- **Payment Log:** Document your incoming payments and track remaining balances based on a customizable default daily wage.
- **Analytics:** View total earned, pending balance, and monthly summaries via the Personal Dashboard and Stats page.
- **Self PDF Export:** Export your personal monthly work log to a clean PDF to maintain an accurate personal ledger.

### 🌟 Premium Features
- **Subscription Models:** Monthly, Half-Yearly, Yearly, and Lifetime plans available for power users needing extended features.

---

## 🛠️ Tech Stack

**DailyWork** leverages modern web technologies to ensure a fast, reliable, and maintainable codebase.

- **Frontend Framework:** React 18, TypeScript, Vite
- **UI Components:** Shadcn UI, Radix UI Primitives
- **Styling:** Tailwind CSS (with `tailwind-merge` and `clsx` for dynamic classes), Framer Motion for animations
- **Backend & Authentication:** Firebase Firestore (NoSQL Database), Firebase Authentication (Google OAuth & Email/Password)
- **Data Visualizations:** Recharts for interactive charts and graphs
- **PDF Generation:** jsPDF & jspdf-autotable for client-side document generation
- **State Management:** React Context API (Auth, Language)
- **Testing:** Vitest (Unit Testing), Playwright (E2E Testing)
- **Linting:** ESLint

---

## 📱 Android App (Native)

A fully native Android app matching the web application's UI and features is included in the `/android-app` directory. It is built using **Kotlin** and **Jetpack Compose**.

### Running the Android App

1. Open Android Studio.
2. Select **Open an existing project**.
3. Navigate to and select the `/android-app` directory.
4. Let Gradle sync and build the project.
5. Run the app on an emulator or physical device.

### Android Build & Signing

The Android project is pre-configured with a keystore for signing Release builds.

- **Keystore Path**: `android-app/keystore/release.jks`
- **Keystore Password**: `android`
- **Key Alias**: `my-key-alias`
- **Key Password**: `android`

To build a signed APK or AAB from the command line:

```bash
cd android-app
# Build APK
./gradlew assembleRelease
# Build AAB (App Bundle)
./gradlew bundleRelease
```

### CI/CD for Android
A GitHub Actions workflow is set up in `.github/workflows/android-build.yml`. It automatically builds a signed APK and AAB on every push to the `main` or `master` branch. The output artifacts can be downloaded directly from the GitHub Actions run summary.

---

## ⚙️ Installation & Setup (Web)

Follow these steps to run the web application locally.

### Prerequisites
- Node.js (v18 or higher recommended)
- `npm` or `bun` installed on your machine.
- A Firebase project with Firestore and Authentication enabled.

### 1. Clone the repository
```bash
git clone <repository-url>
cd dailywork
```

### 2. Install dependencies
Using `npm`:
```bash
npm install --legacy-peer-deps
```
*Note: The `--legacy-peer-deps` flag is recommended to resolve known peer dependency conflicts (e.g., with `date-fns`).*

Alternatively, using `bun`:
```bash
bun install
```

### 3. Environment Variables
Create a `.env` file in the root directory. You can use `.env.example` as a template.
```env
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain
VITE_FIREBASE_PROJECT_ID=your_project_id
VITE_FIREBASE_STORAGE_BUCKET=your_storage_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=your_messaging_sender_id
VITE_FIREBASE_APP_ID=your_app_id
```

### 4. Run the Development Server
Using `npm`:
```bash
npm run dev &
```
Using `bun`:
```bash
bun run dev &
```
The application will be available at `http://localhost:5173`.

---

## 📂 Project Structure Overview

```
dailywork/
├── public/                 # Static assets
├── src/
│   ├── components/         # Reusable UI components (Shadcn UI, Custom layouts)
│   ├── contexts/           # Global React Contexts (AuthContext, LanguageContext)
│   ├── hooks/              # Custom React Hooks
│   ├── lib/                # Utility functions (e.g., cn for Tailwind, Firebase init)
│   ├── pages/              # Main application pages/routes
│   ├── App.tsx             # Root application component and routing setup
│   ├── index.css           # Global Tailwind CSS imports
│   └── main.tsx            # React application entry point
├── .env.example            # Example environment variables
├── package.json            # Project dependencies and scripts
├── tailwind.config.ts      # Tailwind CSS configuration
├── vite.config.ts          # Vite configuration
└── vitest.config.ts        # Vitest testing configuration
```

---

## 🧪 Testing

The project uses `Vitest` for unit testing.
To run the tests:
```bash
npm run test &
# or
bun run test &
```

For End-to-End (E2E) testing, `Playwright` is configured:
```bash
npx playwright test &
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check the [issues page](https://github.com/your-repo/issues) if you want to contribute.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License.

---
*Built with ❤️ by Dharmjeet*
