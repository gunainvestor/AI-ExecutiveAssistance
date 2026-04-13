package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.toEntity
import com.execos.data.local.toEntry
import com.execos.data.model.EnergyEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnergyRepository @Inject constructor(
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.energyDao()

    fun observeRecent(uid: String, limit: Long = 60): Flow<List<EnergyEntry>> =
        dao.observeRecent(limit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            .map { list -> list.map { it.toEntry() } }

    suspend fun saveEnergy(uid: String, entry: EnergyEntry) {
        val id = entry.date.replace("-", "")
        dao.upsert(entry.toEntity(id))
    }
}
