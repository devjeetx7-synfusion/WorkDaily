package com.synfusion.attendance.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val role: String, // "contractor" or "personal"
    val dailyWage: Double = 500.0,
    val countryCode: String = "IN",
    val currencyCode: String = "INR",
    val createdAt: Long = System.currentTimeMillis()
)
