For Task 1: Fix Long-Press BottomNavigationBar Full-Screen Shadow/Overlay
- In `com/synfusion/attendance/ui/components/FloatingBottomNavigation.kt`, find the `AnimatedVisibility` block around `Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))` that appears when `showSelector` is true. Remove it completely. This causes the full-screen dimming overlay. No other changes are needed for the shadow since the long-press selector arc items already have their own localized `shadow` modifier.

For Task 2: Full Backup & Restore Audit + Fix + Custom Backup Name
Audit Findings:
1. **Restore Issue:** When `restoreManager.performRestore` finishes, `operationSuccess` is set, and a restart button appears. `restoreManager.triggerCleanRestart()` exits the process. The restore itself does swap the database safely using `TempDatabaseManager.swapToProduction`. However, in `BackupRestoreBottomSheet.kt`, `isOperating` stays `true` unless `report == null`, but in success case, `triggerCleanRestart` is called, meaning it might restart before showing success? Wait: `triggerCleanRestart` is conditionally called ONLY IF NOT `isSuccess`. Let's check `BackupRestoreBottomSheet.kt`:
```kotlin
                                        val report = restoreManager.performRestore(file, mergeMode, progressTracker)
                                        restoreSummaryReport = report
                                        operationSuccess = report != null
                                        if (report == null) {
                                            errorMessage = context.getString(R.string.restore_failed_msg)
                                            isOperating = false
                                        } else {
                                            restoreManager.triggerCleanRestart()
                                        }
```
If `report != null`, it directly calls `triggerCleanRestart()` which kills the app instantly. It NEVER shows the success screen (`isSuccess` UI block) or allows the user to see the success report. The restart should be triggered manually by the user from the success screen, which has a button "Restart App" (`if (isSuccess && operationType == "RESTORE") stringResource(R.string.restart_app)`). We must REMOVE `restoreManager.triggerCleanRestart()` from the success block in `BackupRestoreBottomSheet.kt`, and let `isOperating = false` be set so the success UI is shown.

Wait, looking at `BackupRestoreBottomSheet.kt` line 316:
```kotlin
                    Button(
                        onClick = {
                            operationSuccess = null
                            if (isSuccess && operationType == "RESTORE") {
                                // Trigger app restart on successful restore
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        },
```
So yes, the UI *wants* to show a success report and provide a "Restart App" button. But `triggerCleanRestart()` is killing the app prematurely before the UI state can update.

Wait, `triggerCleanRestart()` does this:
```kotlin
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            val restartIntent = Intent.makeRestartActivityTask(intent.component)
            context.startActivity(restartIntent)
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
```
We should remove `restoreManager.triggerCleanRestart()` from the `performRestore` success callback. Instead, we should do `isOperating = false` so the success UI shows up.
And in `RestoreManager.kt` we see: `DatabaseLifecycleCoordinator.refreshViewModels()` which refreshes the viewmodels. This means that a restart is not even strictly necessary if the UI just refreshes? Or maybe the manual "Restart App" button handles it safely if they want a clean restart.

2. **Custom Backup Name:**
In `BackupRestoreBottomSheet.kt`, instead of directly calling `createBackupLauncher.launch(...)` from the `BackupOptionCard`, I'll show a small dialog (or just replace the content block with a name input field) when they click "Create New Backup".
I'll add state:
```kotlin
var showBackupNameDialog by remember { mutableStateOf(false) }
var customBackupName by remember { mutableStateOf("") }
```
When they click "Create New Backup", set `showBackupNameDialog = true` and initialize `customBackupName` to a default like "WorkDaily_Backup".
When they click "Create Backup" in the dialog:
Sanitize the name: replace invalid chars, trim. If empty, use "WorkDaily_Backup".
Append `_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip` to ensure uniqueness, OR just use the custom name + `.zip` if they prefer. The prompt says: "Example flow: [ My WorkDaily Backup ] -> My WorkDaily Backup.<existing-extension>".
So `"${customBackupName.trim()}.zip"`. I will sanitize it to remove `\ / : * ? " < > |`.

3. **Backup Issue:**
Is there any backup root cause issue?
```kotlin
                    try {
                        val tempFile = File(context.cacheDir, "AttendanceBackup_temp.zip")
                        backupManager.performBackup(tempFile, progressTracker)
                        // Stream temp zip bytes to output URI via SAF
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                tempFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        // ...
```
Wait! `backupManager.performBackup` does the backup.
But let's look at `BackupManager.performBackup`:
```kotlin
                if (outputZipFile.exists()) outputZipFile.delete()
                BackupProvider.compressFolder(tempDir, outputZipFile, logger)
```
Wait, in `BackupProvider.compressFolder`:
```kotlin
    fun compressFolder(sourceDir: File, zipFile: File, logger: BackupLogger) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zipDir(sourceDir, sourceDir, zos, logger)
        }
    }
```
This correctly creates `outputZipFile`.
Is there a problem with the generated zip file?
Wait, the prompt says: "10. Success state is returned only after the file is successfully written."
In `BackupRestoreBottomSheet.kt`, `isOperating = false` is called in the `finally` block. But what if SAF picker returns an existing file and we just overwrite it? SAF will truncate.
But wait! Look at the prompt: "If the architecture supports transactions, use them appropriately." for Restore. For Backup: "Do not create a superficial workaround... find actual root cause... fix Backup... Fix Restore".
Let's re-read the prompt exactly. "Find the Actual Root Cause... Backup side ... Serialization produces valid backup data ... File is saved to the intended location ... Success state is returned only after the file is successfully written."
Wait, look at `TempDatabaseManager.kt`:
```kotlin
            if (prodDbFile.exists()) {
                if (!prodDbFile.renameTo(backupProdDb)) {
                    throw Exception("Failed to backup main production database file.")
                }
            }
```
If `backupProdDb` already exists (maybe from a previous failed restore), `renameTo` will fail!
Wait:
```kotlin
        backupProdDb.delete()
        backupProdWal.delete()
        backupProdShm.delete()
```
It deletes them first, so `renameTo` shouldn't fail.

Let's look at `BackupManager.kt`:
```kotlin
                val finalSize = outputZipFile.length()
                val uncompressedSize = calculateDirSize(tempDir)
                val ratio = if (uncompressedSize > 0) finalSize.toDouble() / uncompressedSize.toDouble() else 1.0

                val updatedManifest = manifestObj.copy(
                    backupSize = finalSize,
                    compressionRatio = ratio
                )
                ManifestManager.writeManifest(manifestFile, updatedManifest) // Updated locally
```
Wait! `ManifestManager.writeManifest(manifestFile, updatedManifest)` updates the manifest in `tempDir`, BUT `BackupProvider.compressFolder(tempDir, outputZipFile, logger)` was ALREADY CALLED before this!
So the zip file contains the OLD manifest with `backupSize = 0L`!
This is a serialization root cause! The manifest in the ZIP does not have the final size or compression ratio!
To fix: Move `BackupProvider.compressFolder(tempDir, outputZipFile, logger)` AFTER updating the manifest! Wait, we don't know the final zip size until we compress it.
So we should estimate or just compress twice? No, we can just remove `finalSize` from the manifest in the zip, or compute it later. But wait, `backupSize` is validated in `RestoreManager`:
```kotlin
                val freeSpace = context.filesDir.usableSpace
                val backupSize = manifest.backupSize
                if (freeSpace < backupSize * 2) {
```
If `backupSize` is 0 in the zip's manifest, `freeSpace < 0 * 2` is false, but it means the validation doesn't know the real size.
Let me check `BackupManager.kt`:
```kotlin
                progressTracker.updateProgress(90f, "Compressing backup archive...")
                if (outputZipFile.exists()) outputZipFile.delete()
                BackupProvider.compressFolder(tempDir, outputZipFile, logger)

                val finalSize = outputZipFile.length()
                val uncompressedSize = calculateDirSize(tempDir)
                val ratio = if (uncompressedSize > 0) finalSize.toDouble() / uncompressedSize.toDouble() else 1.0

                val updatedManifest = manifestObj.copy(
                    backupSize = finalSize,
                    compressionRatio = ratio
                )
                ManifestManager.writeManifest(manifestFile, updatedManifest) // Updated locally
```
Yes, `ManifestManager.writeManifest` writes the updated manifest to `tempDir`, but `outputZipFile` is already zipped! So the updated manifest is NEVER inside the zip.
To fix this, we can just use `uncompressedSize` as the `backupSize` in the manifest BEFORE zipping, or we don't need the exact zip size in the manifest, just the uncompressed size is better for restore storage check anyway!

What else for Restore?
In `RestoreManager.kt`:
```kotlin
        val manifest = ManifestManager.readManifest(manifestFile)
        if (manifest == null) {
            logger.error("Restore failed: Manifest not found.")
            progressTracker.updateState(RestoreState.RESTORE_FAILED)
            return@withContext null
        }
```
Wait, `BackupValidator.validateBackupDir` reads the checksums:
```kotlin
                for ((relPath, expectedHash) in checksumMap) {
                    val actualFile = File(extractedDir, relPath)
                    if (!ChecksumManager.verifyChecksum(actualFile, expectedHash)) {
```
But the checksums were computed on the files in `tempDir` DURING backup.
In `BackupManager.kt`:
```kotlin
                val checksumMap = mutableMapOf<String, String>()
                collectChecksumsRecursive(tempDir, tempDir, checksumMap)
                val checksumFile = File(metaDir, "checksums.json")
                checksumFile.writer().use { Gson().toJson(checksumMap, it) }
```
Wait! `collectChecksumsRecursive` collects checksums of everything, but THEN we create `manifest.json`. Wait, `collectChecksumsRecursive` excludes `manifest.json` and `checksums.json`. But if any file is modified after checksum generation, it would fail...
Wait, what about `RestoreResultStore`? In `RestoreManager.kt`:
```kotlin
                    RestoreResultStore.saveReport(
                        context = context,
                        report = report,
                        backupDate = manifest.backupDate,
                        backupSize = manifest.backupSize,
                        databaseVersion = manifest.databaseVersion,
                        isMerge = merge
                    )
```
It saves the report. Then `BackupRestoreBottomSheet.kt` accesses `restoreSummaryReport` from `val report = restoreManager.performRestore(file, mergeMode, progressTracker)`.
Wait, in `BackupRestoreBottomSheet.kt`:
```kotlin
                                        val report = restoreManager.performRestore(file, mergeMode, progressTracker)
                                        restoreSummaryReport = report
                                        operationSuccess = report != null
                                        if (report == null) {
                                            errorMessage = context.getString(R.string.restore_failed_msg)
                                            isOperating = false
                                        } else {
                                            restoreManager.triggerCleanRestart()
                                        }
```
Wait, `restoreManager.triggerCleanRestart()` KILLS THE PROCESS (`System.exit(0)`).
If the process is killed immediately, the UI in `BackupRestoreBottomSheet` NEVER updates to show the "Success" screen. The user just sees the app crash/restart immediately.
If we remove `restoreManager.triggerCleanRestart()`, we should set `isOperating = false`, so it shows the success state. THEN, when the user clicks "Restart App" in the success state, it does:
```kotlin
                        onClick = {
                            operationSuccess = null
                            if (isSuccess && operationType == "RESTORE") {
                                // Trigger app restart on successful restore
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        },
```
Wait, the manual restart button doesn't do `System.exit(0)`, it just restarts the activity. But since the DB was swapped, we probably DO want `System.exit(0)` or `restoreManager.triggerCleanRestart()` to ensure the singleton `AttendanceDatabase` is fully recreated if `Activity` recreation isn't enough. BUT the user MUST see the success screen first! So I will call `triggerCleanRestart()` ONLY when the user clicks the "Restart App" button on the success screen!
Wait, in `RestoreManager.kt`, `AttendanceDatabase.getInstance(context)` is called after swap. Since `AttendanceDatabase.resetInstance()` was called inside `swapToProduction`, calling `getInstance` will recreate it. And `DatabaseLifecycleCoordinator.refreshViewModels()` refreshes ViewModels. So we MIGHT NOT EVEN NEED to restart! The app can just continue working! BUT, the button says "Restart App" if `operationType == "RESTORE"`. We can let the button restart the app. We just need to replace the button's onClick with `restoreManager.triggerCleanRestart()` for safety, or keep the intent + `Activity?.finish()`. Let's just fix the premature kill:
Change:
```kotlin
                                        if (report == null) {
                                            errorMessage = context.getString(R.string.restore_failed_msg)
                                            isOperating = false
                                        } else {
                                            isOperating = false
                                            // DO NOT call restoreManager.triggerCleanRestart() here!
                                        }
```
And another problem in `BackupRestoreBottomSheet.kt`:
```kotlin
                    Button(
                        onClick = {
                            operationSuccess = null
                            if (isSuccess && operationType == "RESTORE") {
                                // Trigger app restart on successful restore
                                restoreManager.triggerCleanRestart()
                            } else {
                                onDismiss()
                            }
                        },
```
Let's check the existing code for the Done/Restart button.
```kotlin
                    Button(
                        onClick = {
                            operationSuccess = null
                            if (isSuccess && operationType == "RESTORE") {
                                // Trigger app restart on successful restore
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        },
```
I'll replace the inside of `if (isSuccess && operationType == "RESTORE")` with `restoreManager.triggerCleanRestart()`.

Wait, what about the Backup Name Dialog?
I need to add a small UI for it in `BackupRestoreBottomSheet.kt`.
```kotlin
    var showBackupNameDialog by remember { mutableStateOf(false) }
    var customBackupName by remember { mutableStateOf("") }
```
When they click "Create New Backup":
```kotlin
                        onClick = {
                            customBackupName = "WorkDaily_Backup"
                            showBackupNameDialog = true
                        }
```
And add an `AlertDialog` for `showBackupNameDialog`:
```kotlin
    if (showBackupNameDialog) {
        AlertDialog(
            onDismissRequest = { showBackupNameDialog = false },
            title = { Text(stringResource(R.string.create_new_backup)) },
            text = {
                OutlinedTextField(
                    value = customBackupName,
                    onValueChange = { customBackupName = it },
                    label = { Text("Backup Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    var sanitized = customBackupName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    if (sanitized.isEmpty()) {
                        sanitized = "WorkDaily_Backup"
                    }
                    if (!sanitized.endsWith(".zip", ignoreCase = true)) {
                        sanitized += ".zip"
                    }
                    showBackupNameDialog = false
                    createBackupLauncher.launch(sanitized)
                }) {
                    Text("Create Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
```
This perfectly matches the prompt's requirement for a custom backup name input.

Let me review the Prompt's "Backup Issue" section:
"Do not create a superficial workaround. If the issue is caused by: wrong file path, incorrect file extension, broken serialization, incorrect parsing, async timing, database transaction, stale state, incorrect storage access, file picker result handling, incorrect overwrite behavior, missing refresh, lifecycle issue, error handling, invalid backup structure then fix that actual root cause."
Wait, "invalid backup structure" or "broken serialization".
As I found: `BackupManager.kt` compresses the folder BEFORE writing the final `manifest.json`. So the `manifest.json` in the zip is the ONE generated BEFORE `ManifestManager.writeManifest(manifestFile, updatedManifest)`!
Wait, `ManifestManager.generateManifest` is called BEFORE compression:
```kotlin
                val manifestObj = ManifestManager.generateManifest(...)
                ManifestManager.writeManifest(manifestFile, manifestObj)
```
Then `BackupProvider.compressFolder(tempDir, outputZipFile, logger)`
Then `updatedManifest = manifestObj.copy(backupSize = finalSize, ...)`
Then `ManifestManager.writeManifest(manifestFile, updatedManifest)`
So the zip file contains a valid `manifest.json` with `backupSize = 0L`!
Is this breaking the restore?
In `RestoreManager.kt`:
```kotlin
                val freeSpace = context.filesDir.usableSpace
                val backupSize = manifest.backupSize
                if (freeSpace < backupSize * 2) {
                    logger.error("Restore failed: Insufficient device storage space...")
                    throw Exception("Insufficient storage space to perform restore.")
                }
```
If `backupSize = 0L`, it will say `freeSpace < 0` which is false, so it doesn't fail. But this is wrong because it doesn't actually check free space correctly.
To fix this, in `BackupManager.kt`:
Instead of `outputZipFile.length()` for `backupSize` inside the manifest, just use `uncompressedSize` since the manifest belongs INSIDE the zip.
```kotlin
                val uncompressedSize = calculateDirSize(tempDir)
                val manifestObj = ManifestManager.generateManifest(
                    // ...
                    backupSize = uncompressedSize,
                )
                ManifestManager.writeManifest(manifestFile, manifestObj)

                progressTracker.updateProgress(90f, "Compressing backup archive...")
                if (outputZipFile.exists()) outputZipFile.delete()
                BackupProvider.compressFolder(tempDir, outputZipFile, logger)
```
Then remove the second `writeManifest` because we already wrote it, and it's inside the zip!

Let's double check if there's any other Backup root cause.
Wait, `BackupRestoreBottomSheet.kt` line 77:
```kotlin
                    try {
                        val tempFile = File(context.cacheDir, "AttendanceBackup_temp.zip")
                        backupManager.performBackup(tempFile, progressTracker)

                        // Stream temp zip bytes to output URI via SAF
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                tempFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
```
What if `uri` is opened with "w" mode and we just overwrite? `openOutputStream(uri)` truncates by default in SAF. So this is fine.
Wait, "Success state is returned only after the file is successfully written."
In `BackupRestoreBottomSheet.kt`, `operationSuccess = true` is set AFTER `copyTo(outputStream)`. This is correct.

Wait! The prompt also mentions: "Duplicate Backup Names - If the destination already contains the same filename: Handle it safely. Do not silently corrupt or unexpectedly overwrite an existing backup. Use the existing application behavior if one already exists; otherwise implement a safe conflict strategy."
SAF (`CreateDocument`) handles duplicate names automatically by appending `(1)` to the filename. So we don't need to do anything manually for SAF. SAF handles it safely. BUT, if they enter a name, we just pass it to `CreateDocument`.

Wait! "File picker result handling" - Is there any issue with `pickRestoreLauncher`?
```kotlin
                    try {
                        val tempFile = File(context.cacheDir, "RestoreTemp.zip")
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                FileOutputStream(tempFile).use { fos ->
                                    inputStream.copyTo(fos)
                                }
                            }
                        }
```
This is also fine.

Let's re-verify: "Do not create a superficial workaround. If the issue is caused by: wrong file path ... missing refresh, lifecycle issue ... fix that actual root cause. Do not duplicate the entire Backup/Restore system just to make the button appear functional."
Ah! Is there a problem in `BackupRegistry.kt` or `DatabaseLifecycleCoordinator.kt` with "missing refresh"?
Look at `RestoreManager.kt`:
```kotlin
                    // 5. Unlock database swap state
                    AttendanceDatabase.endSwap()

                    // 6. Ensure final record counts in the restored prod database
                    val finalProdDb = AttendanceDatabase.getInstance(context)
                    val finalUser = finalProdDb.userDao().getUser()
                    val finalAttendance = finalProdDb.attendanceDao().getPersonalAttendance()

                    progressTracker.updateState(RestoreState.RESTORE_SUCCESS)
```
Wait! `DatabaseLifecycleCoordinator.refreshViewModels()` is NOT CALLED in `RestoreManager.kt`!
Wait, in `BackupRegistry.kt`:
```kotlin
    override suspend fun importModule(context: Context, backupWorkingDir: File, merge: Boolean, logger: BackupLogger): Map<String, Int> {
        val tempDb = TempDatabaseManager.createTempDatabase(context)
        try {
            val counts = JsonImporter.importDatabase(tempDb, backupWorkingDir, merge, logger)

            // Redesigned Restore Lifecycle Sequence:
            // 1. Stop active collectors & cancel restore-sensitive jobs
            DatabaseLifecycleCoordinator.stopAndCancelEverything()

            // 2. Wait for all database transactions/queries to finish/cancel
            kotlinx.coroutines.delay(300)

            // 3. Close the temporary database safely
            tempDb.close()

            // 4. Swap data to production database securely (this closes production db via resetInstance() and renames files)
            TempDatabaseManager.swapToProduction(context, tempDb)

            // 5. Recreate production database instance cleanly
            AttendanceDatabase.getInstance(context)

            // 6. Refresh ViewModels (re-query data and trigger new flow subscription collections)
            DatabaseLifecycleCoordinator.refreshViewModels()

            return counts
        } finally {
            tempDb.close()
        }
    }
```
Wait! `BackupRegistry.kt` implements `importModule` for `DatabaseBackupModule`.
BUT `RestoreManager.kt` DOES NOT CALL `DatabaseBackupModule.importModule`!
Let's look at `RestoreManager.kt`:
```kotlin
                progressTracker.updateState(RestoreState.IMPORTING)
                progressTracker.updateProgress(50f, "Importing database atomically...")
                val tempDb = TempDatabaseManager.createTempDatabase(context)

                try {
                    val dbCounts = JsonImporter.importDatabase(tempDb, tempExtractDir, merge = false, logger)
                    // ... merges data ...
                    // 1. Stop active collectors & cancel restore-sensitive jobs
                    DatabaseLifecycleCoordinator.stopAndCancelEverything()

                    // 2. Lock and close production database completely to prevent any queries from racing
                    AttendanceDatabase.beginSwap()

                    // 3. Close the temporary database safely before swapping
                    tempDb.close()

                    // 4. Swap data to production database securely
                    progressTracker.updateState(RestoreState.REPLACING_DATABASE)
                    progressTracker.updateProgress(75f, "Applying atomic database swap...")
                    TempDatabaseManager.swapToProduction(context, tempDb)

                    // Execute OTHER registry modules
                    val modules = BackupRegistry.getModules()
                    modules.forEach { module ->
                        if (module.name != "Database") {
                            // ...
                        }
                    }

                    // 5. Unlock database swap state
                    AttendanceDatabase.endSwap()

                    // ...
```
Notice how `RestoreManager.kt` manually implements the DB restore logic, COMPLETELY IGNORING `DatabaseBackupModule.importModule` and NEVER calling `DatabaseLifecycleCoordinator.refreshViewModels()`!
Ah!!! The root cause of the missing refresh is that `DatabaseLifecycleCoordinator.refreshViewModels()` is NEVER CALLED in `RestoreManager.kt`!
So to fix this: we should call `DatabaseLifecycleCoordinator.refreshViewModels()` right after `AttendanceDatabase.endSwap()` and `AttendanceDatabase.getInstance(context)`!
Or wait, if `refreshViewModels()` is called, we don't even need to restart the app!
But the user can still click "Done" to dismiss the sheet, and the app will just show the updated data!
This perfectly aligns with "ensure current screens reload the restored values ... ensure no restart is unnecessarily required if the architecture can refresh normally".
Yes!
So:
1. In `RestoreManager.kt`, call `DatabaseLifecycleCoordinator.refreshViewModels()` at the end of `performRestore`.
2. In `BackupRestoreBottomSheet.kt`, remove `restoreManager.triggerCleanRestart()` from the auto-execution (when `report != null`). Just set `isOperating = false`.
3. In `BackupRestoreBottomSheet.kt`, change the success button from "Restart App" to "Done", and it should just `onDismiss()`.
Wait, in `BackupRestoreBottomSheet.kt`:
```kotlin
                    Button(
                        onClick = {
                            operationSuccess = null
                            if (isSuccess && operationType == "RESTORE") {
                                // Trigger app restart on successful restore
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        },
```
I can just change the button click to ALWAYS `onDismiss()`. Since `refreshViewModels()` works, we don't need to restart!
Let's double check if `triggerCleanRestart` is still needed. The prompt says "ensure no restart is unnecessarily required if the architecture can refresh normally". So yes, we should rely on `refreshViewModels()`!

Let's review the full plan.
