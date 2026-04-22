package com.execos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.execos.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query(
        """
        SELECT * FROM goals
        WHERE userId = :userId AND periodType = :periodType AND periodKey = :periodKey
        ORDER BY rank ASC
        """,
    )
    fun observeGoals(userId: String, periodType: String, periodKey: String): Flow<List<GoalEntity>>

    @Query(
        """
        SELECT * FROM goals
        WHERE userId = :userId AND periodType = :periodType AND periodKey = :periodKey
        ORDER BY rank ASC
        """,
    )
    suspend fun getGoalsSnapshot(userId: String, periodType: String, periodKey: String): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

    @Query("DELETE FROM goals WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)
}

