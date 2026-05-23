package com.dailywork.attedance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_actions")
data class SyncActionEntity(
    @PrimaryKey val id: String,
    val actionType: String,
    val payload: String,
    val isSynced: Boolean,
    val createdAt: Long
)
