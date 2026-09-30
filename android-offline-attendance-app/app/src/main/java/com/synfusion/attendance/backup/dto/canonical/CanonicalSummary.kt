package com.synfusion.attendance.backup.dto.canonical

data class CanonicalSummary(
    val id: String,
    val monthId: String,
    val role: String,
    val totalPresent: Double,
    val totalWages: Double,
    val totalAdvance: Double,
    val todayPresentCount: Double,
    val todayAbsentCount: Double
)
