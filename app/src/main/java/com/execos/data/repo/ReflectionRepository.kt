package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.toEntity
import com.execos.data.local.toItem
import com.execos.data.model.ReflectionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionRepository @Inject constructor(
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.reflectionDao()

    fun observeRecent(uid: String, limit: Long = 30): Flow<List<ReflectionItem>> =
        dao.observeRecent(uid, limit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            .map { list -> list.map { it.toItem() } }

    suspend fun saveReflection(uid: String, item: ReflectionItem) {
        val id = item.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(item.copy(id = id).toEntity(userId = uid, id = id))
    }
}
