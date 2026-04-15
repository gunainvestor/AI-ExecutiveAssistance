package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM reflections WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun observeRecent(userId: String, limit: Int): Flow<List<ReflectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReflectionEntity)

    @Query("SELECT * FROM reflections WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSnapshot(userId: String, limit: Int): List<ReflectionEntity>
}
