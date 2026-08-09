package com.synfusion.attendance.data.daos

import androidx.room.*
import com.synfusion.attendance.data.entities.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummaryById(id: String): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE id = :id")
    fun getSummaryByIdFlow(id: String): Flow<SummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)

    @Query("SELECT * FROM summaries")
    suspend fun getAllSummaries(): List<SummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<SummaryEntity>)

    @Query("DELETE FROM summaries")
    suspend fun clearAll()
}
