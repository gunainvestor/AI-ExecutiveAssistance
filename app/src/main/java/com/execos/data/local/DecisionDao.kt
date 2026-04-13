package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.DecisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY date DESC LIMIT 200")
    fun observeAll(): Flow<List<DecisionEntity>>

    @Query(
        "SELECT * FROM decisions WHERE date >= :start AND date <= :end ORDER BY date DESC",
    )
    fun observeBetween(start: String, end: String): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DecisionEntity)
}
