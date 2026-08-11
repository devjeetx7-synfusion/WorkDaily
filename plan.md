1.  **Refactor Developer Contact UI:**
    - Update `DeveloperContactBottomSheet` in `SettingsScreen.kt` to reflect "Dharmjeet Singh" and "SynFusion".
    - Introduce Coil `AsyncImage` for offline-safe loading of the developer profile image (`https://i.postimg.cc/tRFX32BV/IMG-20260724-202445.png`).
    - Add localized string resources (or hardcoded fallbacks per design constraints) for the contact section elements.
    - Set up social intent handlers wrapped in `try-catch` to avoid ActivityNotFound exceptions.

2.  **Create Dedicated Screens for Help and Legal Info:**
    - Create `LegalAndHelpScreens.kt` featuring `HowToUseScreen`, `PrivacyPolicyScreen`, and `TermsConditionsScreen`.
    - These screens will utilize standard WorkDaily theming, TopAppBars, and vertically scrollable columns.
    - Add complete localized text blocks (English and Hindi) reflecting only actual offline app behavior to `strings.xml` and `values-hi/strings.xml`.

3.  **Refactor Navigation for Settings Screen:**
    - Update `DashboardScreen.kt` to include routes for `how_to_use`, `privacy_policy`, and `terms_conditions` within its `NavHost`.
    - Modify the signature of `SettingsScreen.kt` to accept an `onNavigate` lambda, allowing click actions on Help & Support to open dedicated screens rather than old, clunky bottom sheets.
    - Clean up removed bottom sheet logic (`showHowToUseSheet`, etc.) in `SettingsScreen.kt`.

4.  **Execute Testing and Pre-commit Checks:**
    - Run Gradle compile, unit tests, and the `pre_commit_instructions` tool to ensure tests pass and changes align with AGENTS.md conventions.
