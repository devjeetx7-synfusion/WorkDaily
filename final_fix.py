import re

file_path = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/SettingsScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Just in case, let's fix any remaining `show...Sheet` that isn't cleanly handled by replacing `show...Sheet = true`
content = content.replace('showHowToUseSheet = true', 'onNavigate("how_to_use")')
content = content.replace('showPrivacySheet = true', 'onNavigate("privacy_policy")')
content = content.replace('showTermsSheet = true', 'onNavigate("terms_conditions")')

with open(file_path, 'w') as f:
    f.write(content)
