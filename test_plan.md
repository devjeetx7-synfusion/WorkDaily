1. **Fix Long-Press BottomNavigationBar Full-Screen Shadow/Overlay:**
   - In `FloatingBottomNavigation.kt`, find the `AnimatedVisibility` block around `Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))` that appears when `showSelector` is true.
   - Remove this `AnimatedVisibility` block completely. This overlay creates the full-screen grey dimming effect, which is unwanted. The local shadow for the expanded arc is already managed via the `shadow` modifier on the items in the long-press arc selector.
   - I will check if `showSelector` needs any other modifications.

2. **Add Custom Backup Name & Fix Backup/Restore:**
   - Currently, `createBackupLauncher` is launched with a hardcoded format `"AttendanceBackup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip"`.
   - I will modify `BackupRestoreBottomSheet.kt` to introduce a custom backup name input.
   - I will use an `AlertDialog` to prompt for the custom name when "Create New Backup" is clicked.
   - The user will enter a name, say `My WorkDaily Backup`, and it will append `.zip` (with sanitize) and then launch `createBackupLauncher` with that customized filename.
   - I will handle the "Restore" functionality audit. As seen in `BackupRestoreBottomSheet.kt`, when `pickRestoreLauncher` is launched, it uses `tempFile` to receive the restore zip. In `RestoreManager.performRestore`, the `mergeMode` and `progressTracker` are used.
   - The prompt says: "Do not create a superficial workaround. If the issue is caused by: wrong file path, incorrect file extension... etc. fix the actual root cause." Let me look closely at the restore/backup logic to see if there is a bug.
   - Looking at `RestoreManager.kt:187`: `DatabaseLifecycleCoordinator.stopAndCancelEverything()` -> `AttendanceDatabase.beginSwap()` -> `tempDb.close()` -> `TempDatabaseManager.swapToProduction(context, tempDb)` -> `AttendanceDatabase.endSwap()`.
   - Wait, `AttendanceDatabase.beginSwap()` and `AttendanceDatabase.endSwap()` don't seem to be defined in standard Room or standard `AttendanceDatabase` unless they were custom implemented. Let me verify `AttendanceDatabase.kt`.
