package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.toEntity
import com.execos.data.local.toItem
import com.execos.data.model.DecisionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecisionRepository @Inject constructor(
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.decisionDao()

    fun observeDecisions(uid: String): Flow<List<DecisionItem>> =
        dao.observeAll(uid).map { list -> list.map { it.toItem() } }

    fun observeDecisionsInRange(uid: String, start: String, end: String): Flow<List<DecisionItem>> =
        dao.observeBetween(uid, start, end).map { list -> list.map { it.toItem() } }

    suspend fun saveDecision(uid: String, item: DecisionItem) {
        val id = item.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(item.copy(id = id).toEntity(userId = uid, id = id))
    }
}
