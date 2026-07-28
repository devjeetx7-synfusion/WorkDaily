package com.synfusion.attendance.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val aadhaar: String = "",
    val dailyWage: Double = 500.0,
    val age: Int = 18,
    val workType: String = "Helper",
    val createdAt: Long = System.currentTimeMillis()
)
