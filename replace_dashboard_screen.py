import re

file_path = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/DashboardScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Modify SettingsScreen call in DashboardScreen:
# From:
# SettingsScreen(
#     viewModel = settingsViewModel,
#     onLogout = onLogout,
#     scrollState = settingsScrollState
# )
# To:
# SettingsScreen(
#     viewModel = settingsViewModel,
#     onLogout = onLogout,
#     onNavigate = { route -> bottomNavController.navigate(route) },
#     scrollState = settingsScrollState
# )

settings_call_pattern = r'SettingsScreen\(\s*viewModel = settingsViewModel,\s*onLogout = onLogout,\s*scrollState = settingsScrollState\s*\)'
new_settings_call = """SettingsScreen(
                                viewModel = settingsViewModel,
                                onLogout = onLogout,
                                onNavigate = { route -> bottomNavController.navigate(route) },
                                scrollState = settingsScrollState
                            )"""
content = re.sub(settings_call_pattern, new_settings_call, content)


# Add new composables to the NavHost in DashboardScreen.kt
# They should be imported at the top.
import_screens = "import com.synfusion.attendance.ui.screens.HowToUseScreen\nimport com.synfusion.attendance.ui.screens.PrivacyPolicyScreen\nimport com.synfusion.attendance.ui.screens.TermsConditionsScreen\n"

if 'import com.synfusion.attendance.ui.screens.HowToUseScreen' not in content:
    content = content.replace('package com.synfusion.attendance.ui\n', 'package com.synfusion.attendance.ui\n\n' + import_screens)

# I need to find the `composable("all_time_passbook") { ... }` block and add the new composables right after it.
new_routes = """
            composable("how_to_use") {
                HowToUseScreen(onNavigateBack = { bottomNavController.navigateUp() })
            }
            composable("privacy_policy") {
                PrivacyPolicyScreen(onNavigateBack = { bottomNavController.navigateUp() })
            }
            composable("terms_conditions") {
                TermsConditionsScreen(onNavigateBack = { bottomNavController.navigateUp() })
            }
"""

all_time_passbook_pattern = r'(composable\("all_time_passbook"\)\s*\{.*?\n            \})'
if re.search(all_time_passbook_pattern, content, re.DOTALL):
    content = re.sub(all_time_passbook_pattern, r'\1' + new_routes, content, flags=re.DOTALL)
else:
    print("Could not find all_time_passbook route.")

with open(file_path, 'w') as f:
    f.write(content)

print("Updated DashboardScreen NavHost.")
