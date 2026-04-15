package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.StravaActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<StravaActivityEntity>)

    @Query("SELECT * FROM strava_activities WHERE userId = :userId ORDER BY startDate DESC LIMIT :limit")
    fun observeRecent(userId: String, limit: Int = 50): Flow<List<StravaActivityEntity>>

    @Query("SELECT * FROM strava_activities WHERE userId = :userId ORDER BY startDate DESC LIMIT :limit")
    suspend fun getRecentSnapshot(userId: String, limit: Int = 50): List<StravaActivityEntity>
}

