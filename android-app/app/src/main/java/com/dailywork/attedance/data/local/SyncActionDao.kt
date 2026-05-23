package com.dailywork.attedance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: SyncActionEntity)

    @Update
    suspend fun update(action: SyncActionEntity): Int

    @Query("SELECT * FROM sync_actions WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedActions(): List<SyncActionEntity>

    @Query("DELETE FROM sync_actions WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
