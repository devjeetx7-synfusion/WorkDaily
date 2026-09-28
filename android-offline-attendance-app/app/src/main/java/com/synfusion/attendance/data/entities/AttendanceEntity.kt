package com.synfusion.attendance.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [
        androidx.room.Index(value = ["date"]),
        androidx.room.Index(value = ["workerId"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey
    val id: String, // format: "workerId_date" or "personal_date"
    val workerId: String?, // null for personal
    val date: String, // "YYYY-MM-DD"
    val status: String, // "present" or "absent"
    val type: String, // "full" or "half"
    val reason: String? = null,
    val overtimeHours: Int = 0,
    val note: String = "",
    val advanceAmount: Double = 0.0,
    val wage: Double = 500.0,
    val currencyCode: String = "INR",
    val createdAt: Long = System.currentTimeMillis()
)
