# ProGuard rules for WorkDaily Offline Attendance App

# Preserve reflection signatures and annotations required by Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod,InnerClasses

# Keep Room Database entities, DAOs, and Database implementation
-keep class com.synfusion.attendance.data.entities.** { *; }
-keep class com.synfusion.attendance.data.daos.** { *; }
-keep class com.synfusion.attendance.data.AttendanceDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep all Gson / Backup / DTO models completely
-keep class com.synfusion.attendance.backup.** { *; }
-keep class com.synfusion.attendance.backup.dto.** { *; }
-keep class com.synfusion.attendance.backup.dto.v1.** { *; }
-keep class com.synfusion.attendance.backup.dto.canonical.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep WorkManager Workers
-keep class com.synfusion.attendance.utils.NotificationWorker { *; }
-keep class com.synfusion.attendance.backup.AutoBackupWorker { *; }
-keep class com.synfusion.attendance.backup.DailySafetyBackupWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep Compose ViewModels
-keep class com.synfusion.attendance.viewmodel.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Manifest BroadcastReceivers
-keep class com.synfusion.attendance.utils.BootReceiver { *; }
-keep class com.synfusion.attendance.utils.NotificationAlarmReceiver { *; }

# Keep Coil Image Loading components
-keep class io.coilkt.** { *; }
