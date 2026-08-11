import re

# 1. Fix DashboardScreen.kt `onNavigate` parameter missing typing and type inference
file_path_dashboard = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/DashboardScreen.kt'
with open(file_path_dashboard, 'r') as f:
    dashboard_content = f.read()

# I will cast `route` to String explicitly so Kotlin can infer the right navigate overload.
dashboard_content = dashboard_content.replace('onNavigate = { route -> bottomNavController.navigate(route) }', 'onNavigate = { route: String -> bottomNavController.navigate(route) }')

with open(file_path_dashboard, 'w') as f:
    f.write(dashboard_content)

# 2. Fix SettingsScreen.kt missing Unresolved reference: HowToUseBottomSheet
file_path_settings = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/SettingsScreen.kt'
with open(file_path_settings, 'r') as f:
    settings_content = f.read()

# In SettingsScreen, there are still references to `if (showHowToUseSheet) { HowToUseBottomSheet(...) }`
# I need to remove those completely.
settings_content = re.sub(r'if\s*\(showHowToUseSheet\)\s*\{\s*HowToUseBottomSheet.*?\n\s*\}', '', settings_content, flags=re.DOTALL)
settings_content = re.sub(r'if\s*\(showPrivacySheet\)\s*\{\s*PrivacyPolicyBottomSheet.*?\n\s*\}', '', settings_content, flags=re.DOTALL)
settings_content = re.sub(r'if\s*\(showTermsSheet\)\s*\{\s*TermsConditionsBottomSheet.*?\n\s*\}', '', settings_content, flags=re.DOTALL)

with open(file_path_settings, 'w') as f:
    f.write(settings_content)

# 3. Fix LegalAndHelpScreens.kt `Unresolved reference: how_to_use` stringResource error.
file_path_legal = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/screens/LegalAndHelpScreens.kt'
with open(file_path_legal, 'r') as f:
    legal_content = f.read()

# The error states:
# e: file:///app/android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/screens/LegalAndHelpScreens.kt:22:56 Unresolved reference: how_to_use
# It seems `R.string.how_to_use` doesn't exist in strings.xml or it should be `R.string.information_support` or something else? Wait, let's look at strings.xml to see what it's called.
# It doesn't exist. I will add it to the strings.xml or change it to just strings I know exist.

import xml.etree.ElementTree as ET
tree = ET.parse('./android-offline-attendance-app/app/src/main/res/values/strings.xml')
root = tree.getroot()
strings_dict = {elem.attrib.get('name'): elem.text for elem in root.findall('string')}

missing_strings = []
if 'how_to_use' not in strings_dict: missing_strings.append('how_to_use')
# Let's write missing strings to strings.xml
if missing_strings:
    print("Found missing string reference, let's just add it to strings.xml via update_strings.py logic")
