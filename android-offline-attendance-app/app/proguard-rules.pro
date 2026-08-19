# ProGuard rules for WorkDaily Offline Attendance App

# Keep Room Database entities, DAOs, and Database implementation
-keep class com.synfusion.attendance.data.entities.** { *; }
-keep class com.synfusion.attendance.data.daos.** { *; }
-keep class com.synfusion.attendance.data.AttendanceDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Gson models for Backup/Restore serialization
-keep class com.synfusion.attendance.backup.BackupModels** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep WorkManager Workers
-keep class com.synfusion.attendance.utils.NotificationWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep Compose ViewModels
-keep class com.synfusion.attendance.viewmodel.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Manifest BroadcastReceivers
-keep class com.synfusion.attendance.utils.BootReceiver { *; }
-keep class com.synfusion.attendance.utils.NotificationAlarmReceiver { *; }

# Keep Coil Image Loading components
-keep class io.coilkt.** { *; }
