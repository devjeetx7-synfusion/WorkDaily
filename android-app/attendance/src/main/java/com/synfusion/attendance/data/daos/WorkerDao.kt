package com.synfusion.attendance.data.daos

import androidx.room.*
import com.synfusion.attendance.data.entities.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkersFlow(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY name ASC")
    suspend fun getAllWorkers(): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerById(id: String): WorkerEntity?

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerByIdFlow(id: String): Flow<WorkerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorkerById(id: String)

    @Query("DELETE FROM workers")
    suspend fun clearAll()
}
