package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY impactScore DESC")
    fun observeByDate(date: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE date >= :start AND date <= :end ORDER BY date ASC",
    )
    fun observeBetween(start: String, end: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
