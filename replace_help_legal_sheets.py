import re

file_path = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/SettingsScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Instead of passing `onLogout: () -> Unit`, we should probably pass `onNavigate: (String) -> Unit`
# or modify the click handlers of How to Use, Privacy, Terms.
# They are currently showing bottom sheets! We want to navigate to the new dedicated screens.

# First, modify the SettingsScreen signature:
# @Composable\nfun SettingsScreen(\n    viewModel: OfflineSettingsViewModel,\n    onLogout: () -> Unit,\n    scrollState: ScrollState = rememberScrollState()\n)
settings_sig_pattern = r'fun SettingsScreen\(\s*viewModel: OfflineSettingsViewModel,\s*onLogout: \(\) -> Unit,\s*scrollState: ScrollState = rememberScrollState\(\)\s*\)'
new_settings_sig = 'fun SettingsScreen(\n    viewModel: OfflineSettingsViewModel,\n    onLogout: () -> Unit,\n    onNavigate: (String) -> Unit,\n    scrollState: ScrollState = rememberScrollState()\n)'
content = re.sub(settings_sig_pattern, new_settings_sig, content)

# Next, modify the click handlers for the Settings list items:
# How to Use: `onClick = { showHowToUseSheet = true }` -> `onClick = { onNavigate("how_to_use") }`
# Privacy Policy: `onClick = { showPrivacySheet = true }` -> `onClick = { onNavigate("privacy_policy") }`
# Terms & Conditions: `onClick = { showTermsSheet = true }` -> `onClick = { onNavigate("terms_conditions") }`

content = content.replace('onClick = { showHowToUseSheet = true }', 'onClick = { onNavigate("how_to_use") }')
content = content.replace('onClick = { showPrivacySheet = true }', 'onClick = { onNavigate("privacy_policy") }')
content = content.replace('onClick = { showTermsSheet = true }', 'onClick = { onNavigate("terms_conditions") }')

# Now, we should remove the old BottomSheet definitions (HowToUseBottomSheet, PrivacyPolicyBottomSheet, TermsConditionsBottomSheet)
pattern_remove_sheets = r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun HowToUseBottomSheet.*?\}\s*\}(?=\s*@OptIn|\s*$)'
content = re.sub(pattern_remove_sheets, '', content, flags=re.DOTALL)

with open(file_path, 'w') as f:
    f.write(content)
print("Updated SettingsScreen routing and removed old help bottom sheets.")
