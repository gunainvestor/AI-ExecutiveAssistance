package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.toEntity
import com.execos.data.local.toItem
import com.execos.data.model.WeeklyReviewItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyReviewRepository @Inject constructor(
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.weeklyReviewDao()

    fun observeReviews(uid: String): Flow<List<WeeklyReviewItem>> =
        dao.observeAll().map { list -> list.map { it.toItem() } }

    suspend fun saveReview(uid: String, item: WeeklyReviewItem) {
        val id = item.id.ifBlank { item.weekStart }
        dao.upsert(item.toEntity(id))
    }
}
