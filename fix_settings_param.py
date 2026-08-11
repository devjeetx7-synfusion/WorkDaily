import re

file_path = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/SettingsScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace('onClick = { showDeveloperSheet = true }', 'onClick = { showDeveloperSheet = true }')
# Wait, I already removed how_to_use bottom sheets. But did I actually use `onNavigate` parameter?
# Let's check `SettingsScreen.kt` for `onNavigate` usage.
print(content.count('onNavigate('))
