package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND date = :date ORDER BY impactScore DESC")
    fun observeByDate(userId: String, date: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE userId = :userId AND date >= :start AND date <= :end ORDER BY date ASC",
    )
    fun observeBetween(userId: String, start: String, end: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM tasks WHERE userId = :userId AND date = :date ORDER BY impactScore DESC")
    suspend fun getTasksForDateSnapshot(userId: String, date: String): List<TaskEntity>
}
