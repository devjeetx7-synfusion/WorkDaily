package com.synfusion.attendance.backup.migration

import com.synfusion.attendance.backup.BackupLogger
import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.backup.dto.canonical.*
import com.synfusion.attendance.backup.dto.v1.BackupV1Payload

class V1ToCanonicalMigration : BackupMigration {
    override val fromVersion: Int = 1
    override val toVersion: Int = 1 // Canonical version

    override fun migrate(
        rawPayload: Any,
        manifest: BackupManifest?,
        logger: BackupLogger?
    ): CanonicalBackupPayload {
        val payload = rawPayload as? BackupV1Payload
            ?: throw IllegalArgumentException("Expected BackupV1Payload for V1 migration")

        val preferences = payload.preferences

        val prefCountry = preferences["user_country"]?.toString()
            ?: preferences["country_code"]?.toString()
            ?: preferences["countryCode"]?.toString()
            ?: "IN"

        val prefCurrency = preferences["user_currency"]?.toString()
            ?: preferences["currency_code"]?.toString()
            ?: preferences["currencyCode"]?.toString()
            ?: "INR"

        logger?.info("Migrating V1 backup payload. Preference defaults derived -> country: $prefCountry, currency: $prefCurrency")

        val canonicalUsers = payload.users.map { u ->
            val userCountry = u.countryCode?.takeIf { it.isNotBlank() } ?: prefCountry
            val userCurrency = u.currencyCode?.takeIf { it.isNotBlank() } ?: prefCurrency

            CanonicalUser(
                id = u.id ?: "personal_user",
                name = u.name ?: "User",
                phoneNumber = u.phoneNumber ?: "",
                email = u.email ?: "",
                role = u.role ?: "personal",
                dailyWage = u.dailyWage ?: 500.0,
                countryCode = userCountry,
                currencyCode = userCurrency,
                createdAt = u.createdAt ?: System.currentTimeMillis()
            )
        }

        val primaryUserCurrency = canonicalUsers.firstOrNull()?.currencyCode ?: prefCurrency

        val canonicalAttendance = payload.attendance.map { a ->
            val attCurrency = a.currencyCode?.takeIf { it.isNotBlank() }
                ?: primaryUserCurrency

            CanonicalAttendance(
                id = a.id ?: "att_${a.date ?: "unknown"}",
                workerId = a.workerId,
                date = a.date ?: "",
                status = a.status ?: "present",
                type = a.type ?: "full",
                reason = a.reason,
                overtimeHours = a.overtimeHours ?: 0,
                note = a.note ?: "",
                advanceAmount = a.advanceAmount ?: 0.0,
                wage = a.wage ?: 500.0,
                currencyCode = attCurrency,
                createdAt = a.createdAt ?: System.currentTimeMillis()
            )
        }

        val canonicalSummaries = payload.summaries.map { s ->
            CanonicalSummary(
                id = s.id ?: "${s.role ?: "personal"}_${s.monthId ?: ""}",
                monthId = s.monthId ?: "",
                role = s.role ?: "personal",
                totalPresent = s.totalPresent ?: 0.0,
                totalWages = s.totalWages ?: 0.0,
                totalAdvance = s.totalAdvance ?: 0.0,
                todayPresentCount = s.todayPresentCount ?: 0.0,
                todayAbsentCount = s.todayAbsentCount ?: 0.0
            )
        }

        return CanonicalBackupPayload(
            manifest = manifest,
            users = canonicalUsers,
            attendance = canonicalAttendance,
            summaries = canonicalSummaries,
            preferences = preferences,
            mediaFiles = emptyList()
        )
    }
}
