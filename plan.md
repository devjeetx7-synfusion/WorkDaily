Plan:
1. **Profile Settings Logo**:
   - In `SettingsScreen.kt`, replace the `Icon(randomPersonalIcon)` / `Icon(Icons.Default.Business)` logic inside the circular profile image container with an `Image` or `Icon` showing the WorkDaily App logo. Since there's no explicitly named `logo`, we can use `painterResource(id = R.mipmap.ic_launcher_round)` or `R.mipmap.ic_launcher` (the launcher icon) for the app logo. Wait, it says "Replace the current placeholder/profile icon with the WorkDaily App logo." I will use `painterResource(com.dailywork.attedance.R.mipmap.ic_launcher)`.

2. **Data & Support Section Items**:
   - Ensure "How to Use DailyWork", "Privacy Policy", "Developer Contact" are kept.
   - ADD "Terms & Conditions".
   - The user requested them to open *in-app bottom sheet dialogs* instead of external websites.
   - So I will re-create the boolean state variables for `showHowToUseSheet`, `showPrivacySheet`, `showTermsSheet`, `showDeveloperSheet`.

3. **Bottom Sheet Dialog Systems**:
   - Create private Composables for each bottom sheet inside `SettingsScreen.kt` (or at the bottom of the file).
   - `HowToUseBottomSheet(onDismiss: () -> Unit)`
   - `PrivacyPolicyBottomSheet(onDismiss: () -> Unit)`
   - `TermsConditionsBottomSheet(onDismiss: () -> Unit)`
   - `DeveloperContactBottomSheet(onDismiss: () -> Unit)`

4. **Sheet Contents**:
   - **How To Use**: Text explaining the features exactly as requested.
   - **Privacy Policy**: Text explaining privacy exactly as requested. Includes SynFusion brand and email.
   - **Terms & Conditions**: Text explaining terms exactly as requested.
   - **Developer Contact**: Needs circular logo, SynFusion branding, workdaily.synfusion@gmail.com. Plus 3 social icons horizontally aligned (Instagram, Telegram, Gmail). Use `Icons.Default.Email` for Gmail. Since Instagram/Telegram icons might not exist in standard material icons, I'll either use a fallback `Icon` or text, but the prompt says "circular icon buttons". I will try to use a standard `Share` or `Chat` icon if the specific ones are unavailable, or I will look up `painterResource` if there are drawable resources for them. Wait, if I don't have Instagram/Telegram vectors, I'll use generic placeholders like `Icons.Default.CameraAlt` for Instagram and `Icons.Default.Send` for Telegram, and `Icons.Default.Email` for Gmail.

5. **Safe Intent Handling for Social Links**:
   - For Instagram: `https://www.instagram.com/devjeetx7`
   - For Telegram: `https://t.me/synfusion_coder`
   - For Gmail: `mailto:devjeetx7@gmail.com`
   - Wrap in `try/catch(ActivityNotFoundException) { Toast... }`.

6. **Cleanup**:
   - Remove the `SettingsItem` definitions and unused items. (Already done previously, but I'll make sure they aren't there).
   - Ensure Logout button keeps the `showLogoutDialog` logic that I added earlier.

Let's refine the plan using `set_plan`.
