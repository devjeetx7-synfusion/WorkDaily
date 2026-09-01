package com.synfusion.attendance.backup

import com.google.gson.annotations.SerializedName
import com.synfusion.attendance.data.entities.AttendanceEntity
import com.synfusion.attendance.data.entities.SummaryEntity
import com.synfusion.attendance.data.entities.UserEntity

data class BackupManifest(
    @SerializedName("backupUuid") val backupUuid: String,
    @SerializedName("creationTimestamp") val creationTimestamp: Long,
    @SerializedName("totalBackupDurationMs") val totalBackupDurationMs: Long,
    @SerializedName("compressionRatio") val compressionRatio: Double,
    @SerializedName("appBuildNumber") val appBuildNumber: Long,
    @SerializedName("backupFormatVersion") val backupFormatVersion: Int,

    @SerializedName("backupVersion") val backupVersion: Int,
    @SerializedName("appVersion") val appVersion: String,
    @SerializedName("databaseVersion") val databaseVersion: Int,
    @SerializedName("androidVersion") val androidVersion: Int,
    @SerializedName("deviceModel") val deviceModel: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("backupDate") val backupDate: Long,
    @SerializedName("recordCounts") val recordCounts: Map<String, Int>,
    @SerializedName("totalFiles") val totalFiles: Int,
    @SerializedName("backupSize") val backupSize: Long,
    @SerializedName("featureList") val featureList: List<String>
)

data class BackupDataPayload(
    @SerializedName("users") val users: List<UserEntity> = emptyList(),
    @SerializedName("attendance") val attendance: List<AttendanceEntity> = emptyList(),
    @SerializedName("summaries") val summaries: List<SummaryEntity> = emptyList(),
    @SerializedName("preferences") val preferences: Map<String, Any> = emptyMap()
)

data class DeviceInfo(
    val brand: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: Int,
    val sdkVersion: Int
)

data class AppInfo(
    val packageName: String,
    val appVersion: String,
    val versionCode: Long
)

data class StatisticsInfo(
    val totalBackupDurationMs: Long,
    val backupSuccess: Boolean
)

data class RestoreSummaryReport(
    val attendanceCount: Int,
    val totalAttendanceRecords: Int,
    val profileRestored: Boolean,
    val mediaCount: Int,
    val totalMediaFiles: Int,
    val settingsRestored: Boolean
)
