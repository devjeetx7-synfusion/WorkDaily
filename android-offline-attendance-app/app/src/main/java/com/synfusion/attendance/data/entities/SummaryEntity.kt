package com.synfusion.attendance.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    indices = [
        androidx.room.Index(value = ["monthId", "role"])
    ]
)
data class SummaryEntity(
    @PrimaryKey
    val id: String, // e.g. "personal_YYYY-MM" or "contractor_YYYY-MM"
    val monthId: String, // format "YYYY-MM"
    val role: String, // "personal" or "contractor"
    val totalPresent: Double = 0.0,
    val totalWages: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val todayPresentCount: Double = 0.0,
    val todayAbsentCount: Double = 0.0
)
