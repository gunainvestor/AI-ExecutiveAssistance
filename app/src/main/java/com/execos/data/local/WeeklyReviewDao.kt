package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.WeeklyReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReviewDao {
    @Query("SELECT * FROM weekly_reviews ORDER BY weekStart DESC LIMIT 52")
    fun observeAll(): Flow<List<WeeklyReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeeklyReviewEntity)
}
