package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.entity.GoalEntity
import com.execos.data.model.GoalItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.goalDao()

    fun observeGoals(uid: String, periodType: String, periodKey: String): Flow<List<GoalItem>> =
        dao.observeGoals(uid, periodType, periodKey).map { rows ->
            rows.map { r ->
                GoalItem(
                    id = r.id,
                    periodType = r.periodType,
                    periodKey = r.periodKey,
                    rank = r.rank,
                    title = r.title,
                )
            }
        }

    suspend fun saveGoal(uid: String, goal: GoalItem) {
        val id = goal.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(
            GoalEntity(
                id = id,
                userId = uid,
                periodType = goal.periodType,
                periodKey = goal.periodKey,
                rank = goal.rank,
                title = goal.title,
            ),
        )
    }

    suspend fun deleteGoal(uid: String, id: String) {
        dao.deleteById(uid, id)
    }
}

