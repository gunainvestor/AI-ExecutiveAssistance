package com.execos.data.repo

import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.toEntity
import com.execos.data.local.toItem
import com.execos.data.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.taskDao()

    fun observeTasksForDate(uid: String, date: String): Flow<List<TaskItem>> =
        dao.observeByDate(uid, date).map { list -> list.map { it.toItem() } }

    fun observeTasksInRange(uid: String, start: String, end: String): Flow<List<TaskItem>> =
        dao.observeBetween(uid, start, end).map { list -> list.map { it.toItem() } }

    suspend fun saveTask(uid: String, task: TaskItem) {
        val id = task.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(task.copy(id = id).toEntity(userId = uid, id = id))
    }

    suspend fun deleteTask(uid: String, taskId: String) {
        dao.deleteById(taskId)
    }
}
