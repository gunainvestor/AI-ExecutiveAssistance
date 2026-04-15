package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.EnergyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyDao {
    @Query("SELECT * FROM energy WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun observeRecent(userId: String, limit: Int): Flow<List<EnergyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EnergyEntity)
}
