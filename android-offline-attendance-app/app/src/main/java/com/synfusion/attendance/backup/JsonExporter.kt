package com.synfusion.attendance.backup

import com.google.gson.GsonBuilder
import com.synfusion.attendance.backup.dto.v1.AttendanceV1Dto
import com.synfusion.attendance.backup.dto.v1.BackupV1Payload
import com.synfusion.attendance.backup.dto.v1.SummaryV1Dto
import com.synfusion.attendance.backup.dto.v1.UserV1Dto
import com.synfusion.attendance.data.AttendanceDatabase
import java.io.File
import java.nio.charset.StandardCharsets

object JsonExporter {

    suspend fun exportDataPayload(
        db: AttendanceDatabase,
        preferences: Map<String, Any>,
        outputDir: File,
        logger: BackupLogger
    ): Map<String, Int> {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val recordCounts = mutableMapOf<String, Int>()

        val userEntity = db.userDao().getUser()
        val userDtos = if (userEntity != null) {
            listOf(
                UserV1Dto(
                    id = userEntity.id,
                    name = userEntity.name,
                    phoneNumber = userEntity.phoneNumber,
                    email = userEntity.email,
                    role = userEntity.role,
                    dailyWage = userEntity.dailyWage,
                    countryCode = userEntity.countryCode,
                    currencyCode = userEntity.currencyCode,
                    createdAt = userEntity.createdAt
                )
            )
        } else emptyList()

        val attendanceEntities = db.attendanceDao().getPersonalAttendance()
        val attendanceDtos = attendanceEntities.map { a ->
            AttendanceV1Dto(
                id = a.id,
                workerId = a.workerId,
                date = a.date,
                status = a.status,
                type = a.type,
                reason = a.reason,
                overtimeHours = a.overtimeHours,
                note = a.note,
                advanceAmount = a.advanceAmount,
                wage = a.wage,
                currencyCode = a.currencyCode,
                createdAt = a.createdAt
            )
        }

        val summaryEntities = db.summaryDao().getAllSummaries()
        val summaryDtos = summaryEntities.map { s ->
            SummaryV1Dto(
                id = s.id,
                monthId = s.monthId,
                role = s.role,
                totalPresent = s.totalPresent,
                totalWages = s.totalWages,
                totalAdvance = s.totalAdvance,
                todayPresentCount = s.todayPresentCount,
                todayAbsentCount = s.todayAbsentCount
            )
        }

        recordCounts["users"] = userDtos.size
        recordCounts["attendance"] = attendanceDtos.size
        recordCounts["summaries"] = summaryDtos.size

        val payload = BackupV1Payload(
            users = userDtos,
            attendance = attendanceDtos,
            summaries = summaryDtos,
            preferences = preferences
        )

        val dataFile = File(outputDir, "data.json")
        dataFile.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(payload, writer)
        }

        logger.info("Exported canonical backup payload to data.json (users: ${userDtos.size}, attendance: ${attendanceDtos.size}, summaries: ${summaryDtos.size}, preferences: ${preferences.size}).")
        return recordCounts
    }

    suspend fun exportDatabase(db: AttendanceDatabase, outputDir: File, logger: BackupLogger): Map<String, Int> {
        return exportDataPayload(db, emptyMap(), outputDir, logger)
    }

    fun exportPreferences(outputDir: File, dataStoreValues: Map<String, Any>, sharedPreferencesValues: Map<String, Any>, logger: BackupLogger) {
        // Preferences are now bundled directly into exportDataPayload to avoid multi-pass data.json rewrites
        logger.info("Preferences export merged into single-pass payload.")
    }
}
