package com.synfusion.attendance.backup

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class DailySafetyBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AutoBackupWorker.enqueueAutoBackup(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("WorkDailySafetyBackup", "Daily safety backup trigger failed: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "workdaily_daily_safety_backup"

        fun schedulePeriodicSafetyBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DailySafetyBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
