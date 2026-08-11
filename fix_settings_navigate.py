import re

file_path = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/SettingsScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

# I messed up my previous script `replace_help_legal_sheets.py` probably because it couldn't find the exact match to replace `showHowToUseSheet = true`.
# Let's see the current strings:
print("Occurrences of showHowToUseSheet:")
print(content.count("showHowToUseSheet"))
print("Occurrences of showPrivacySheet:")
print(content.count("showPrivacySheet"))
print("Occurrences of showTermsSheet:")
print(content.count("showTermsSheet"))

# Let's replace the actual `onClick = { showHowToUseSheet = true }` logic
content = content.replace('onClick = { showHowToUseSheet = true }', 'onClick = { onNavigate("how_to_use") }')
content = content.replace('onClick = { showPrivacySheet = true }', 'onClick = { onNavigate("privacy_policy") }')
content = content.replace('onClick = { showTermsSheet = true }', 'onClick = { onNavigate("terms_conditions") }')

# Now let's remove the boolean states because they are no longer needed
content = re.sub(r'var showHowToUseSheet by remember \{ mutableStateOf\(false\) \}', '', content)
content = re.sub(r'var showPrivacySheet by remember \{ mutableStateOf\(false\) \}', '', content)
content = re.sub(r'var showTermsSheet by remember \{ mutableStateOf\(false\) \}', '', content)

with open(file_path, 'w') as f:
    f.write(content)
print("Updated SettingsScreen.kt to use onNavigate")
