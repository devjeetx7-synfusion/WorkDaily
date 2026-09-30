package com.synfusion.attendance.backup.dto.canonical

data class CanonicalAttendance(
    val id: String,
    val workerId: String?,
    val date: String,
    val status: String,
    val type: String,
    val reason: String?,
    val overtimeHours: Int,
    val note: String,
    val advanceAmount: Double,
    val wage: Double,
    val currencyCode: String,
    val createdAt: Long
)
