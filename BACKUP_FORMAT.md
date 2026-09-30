# WorkDaily Backup & Restore Format Specifications

This document defines the authoritative architecture, data contracts, ProGuard rules, migration policies, and verification steps for WorkDaily's offline backup and restore subsystem.

---

## 1. Version Identifiers & Constants

- **`CURRENT_BACKUP_FORMAT_VERSION`**: `1`
- **`CURRENT_DATABASE_VERSION`**: `3`
- **`appVersion`**: Derived dynamically at runtime from `BuildConfig.VERSION_NAME`
- **`appBuildNumber`**: Derived dynamically at runtime from `BuildConfig.VERSION_CODE`

These constants are decoupled. Database version updates do not implicitly invalidate or break backup format compatibility unless the backup contract structure itself changes.

---

## 2. Backup ZIP Archive Specifications

A standard WorkDaily backup package is a single, unencrypted `.zip` archive containing:

```
WorkDaily_Backup_1790258793955.zip
├── manifest.json
├── data.json
└── media/
    └── profile_photos/
        └── avatar.png
```

### 2.1 Manifest Specifications (`manifest.json`)

The manifest contains metadata describing the backup package:

```json
{
  "backupUuid": "ded325f3-a383-481f-9a0c-557a7c12bdc8",
  "creationTimestamp": 1790258793955,
  "totalBackupDurationMs": 67,
  "compressionRatio": 1.0,
  "appBuildNumber": 3,
  "backupFormatVersion": 1,
  "backupVersion": 1,
  "appVersion": "1.0.3",
  "databaseVersion": 3,
  "androidVersion": 36,
  "deviceModel": "V2334",
  "packageName": "com.synfusion.attendance",
  "recordCounts": {
    "users": 1,
    "attendance": 50,
    "summaries": 2,
    "media": 1
  },
  "totalFiles": 4,
  "backupSize": 71069,
  "featureList": [
    "Database",
    "Preferences",
    "Media"
  ]
}
```

### 2.2 Data Payload Specifications (`data.json`)

Serialized in a single pass directly from stable DTO objects (`BackupV1Payload`):

```json
{
  "users": [
    {
      "id": "72d4a90d-9eab-471f-94b5-5884a1e12662",
      "name": "Dharmjeet Singh",
      "phoneNumber": "9334603912",
      "email": "",
      "role": "personal",
      "dailyWage": 400.0,
      "countryCode": "IN",
      "currencyCode": "INR",
      "createdAt": 1790258700000
    }
  ],
  "attendance": [
    {
      "id": "personal_2026-08-01",
      "workerId": null,
      "date": "2026-08-01",
      "status": "present",
      "type": "full",
      "reason": null,
      "overtimeHours": 0,
      "note": "",
      "advanceAmount": 0.0,
      "wage": 400.0,
      "currencyCode": "INR",
      "createdAt": 1790258710000
    }
  ],
  "summaries": [],
  "preferences": {
    "app_language": "hi",
    "dark_mode": true,
    "user_country": "IN",
    "user_currency": "INR"
  }
}
```

---

## 3. R8 / ProGuard Minification Rules

Gson reflection requires stable runtime class names, field names, annotations, and generic signatures. The following rules are enforced in `app/proguard-rules.pro`:

```proguard
# Preserve reflection signatures and annotations required by Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod,InnerClasses

# Keep all Gson / Backup / DTO models completely
-keep class com.synfusion.attendance.backup.** { *; }
-keep class com.synfusion.attendance.backup.dto.** { *; }
-keep class com.synfusion.attendance.backup.dto.v1.** { *; }
-keep class com.synfusion.attendance.backup.dto.canonical.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
```

---

## 4. Legacy Backup Compatibility & Migration Pipeline

WorkDaily guarantees backward compatibility with historical backups created across all public releases.

### 4.1 Legacy Migration Rules
- **User Country**: Use explicit `countryCode` from record if present; otherwise fall back to DataStore `user_country` preference; default to `"IN"`.
- **User Currency**: Use explicit `currencyCode` from record if present; otherwise fall back to DataStore `user_currency` preference; default to `"INR"`.
- **Attendance Currency**: Use explicit `currencyCode` from record if present; otherwise fall back to restored user currency; default to `"INR"`.
- **Fidelity Guarantee**: Historical IDs, dates, wages, overtime hours, advance payments, notes, and timestamps are preserved exactly as stored. Financial values are never recalculated during restore.

---

## 5. Atomic Restore & Rollback Protocol

To prevent data loss or corruption, the restore pipeline executes through strict atomic stages in `RestoreManager`:

1. **Extraction & Preview**: Extract archive into isolated `cacheDir/restore_working_dir`.
2. **Validation**: Validate manifest, backup format version, data file integrity, and record counts.
3. **Storage Check**: Ensure free device disk space is at least $2\times$ the backup size.
4. **Temp DB Import**: Import records into isolated temporary SQLite database and verify imported counts.
5. **Media Restore**: Extract and copy profile photo media files to `context.filesDir/profile_photos` BEFORE preference remapping.
6. **Preferences Restore**: Apply restored DataStore preferences and remap `profile_photo_path` to verified local file paths.
7. **Atomic DB Swap**: Acquire swap lock, close active connections, replace `AttendanceDatabase` file atomically with the verified temp DB file, and re-initialize connections.
8. **Session Activation**: Recover local authentication token and workspace session for the restored user profile.
9. **Rollback Safety**: If any exception occurs prior to or during swap, active production database remains untouched.

---

## 6. Auto-Backup WorkManager Integration

- **Trigger**: Data-mutating operations mark `backup_dirty = true` and enqueue a unique `AutoBackupWorker` job (`workdaily_auto_backup_job`) with a 5-second debounce.
- **Safety Net**: `DailySafetyBackupWorker` runs periodically every 24 hours as a backup safety check.
- **Execution**: `AutoBackupWorker` checks `auto_backup_enabled` and `backup_dirty`. If true, it generates a candidate ZIP file, runs self-validation, copies the validated archive to persistent local storage and user-configured SAF URI, rotates older archives (retaining latest 7 daily backups), and resets `backup_dirty = false`.

---

## 7. Pre-Release Acceptance Checklist

Before any production release build, verify:

1. **Unit Tests**: `./gradlew test` passes 100%.
2. **Release Build**: `./gradlew assembleRelease` compiles cleanly with R8 minification enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
3. **Minified Self-Validation**: Install release APK, populate realistic records, generate backup, and confirm self-validation passes cleanly.
4. **Historical Backup Round-Trip**: Restore a historical legacy backup, verify user profile, 50 attendance records, 2 summaries, and media photo.
5. **Mutation Cycle**: Edit restored data -> generate second backup -> restore second backup -> verify all edited and existing records match expected values.
