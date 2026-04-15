package com.execos.data.remote

import com.execos.data.local.ExecOsDatabase
import com.execos.data.repo.AuthRepository
import com.execos.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes model-requested tools against local Room data (server-side tools pattern, client-hosted).
 */
@Singleton
class ExecutiveToolExecutor @Inject constructor(
    private val db: ExecOsDatabase,
    private val authRepository: AuthRepository,
) {
    suspend fun execute(functionName: String, argumentsJson: String): String = withContext(Dispatchers.IO) {
        runCatching {
            when (functionName) {
                "get_recent_decisions" -> recentDecisionsJson()
                "get_today_priorities" -> todayTasksJson()
                "get_recent_reflections" -> recentReflectionsJson()
                else -> """{"error":"unknown_function","name":"$functionName"}"""
            }
        }.getOrElse { e ->
            """{"error":"${e.message?.replace("\"", "'")}"}"""
        }
    }

    private suspend fun recentDecisionsJson(): String {
        val uid = authRepository.currentUserId()
        val rows = db.decisionDao().getRecentSnapshot(userId = uid, limit = 12)
        val arr = JSONArray()
        rows.forEach { d ->
            arr.put(
                JSONObject().apply {
                    put("title", d.title)
                    put("date", d.date)
                    put("confidence", d.confidence)
                    put("final_decision", d.finalDecision.take(400))
                },
            )
        }
        return JSONObject().put("decisions", arr).toString()
    }

    private suspend fun todayTasksJson(): String {
        val uid = authRepository.currentUserId()
        val today = Dates.todayIso()
        val rows = db.taskDao().getTasksForDateSnapshot(userId = uid, date = today)
        val arr = JSONArray()
        rows.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("title", t.title)
                    put("impact", t.impactScore)
                    put("completed", t.completed)
                    put("notes", t.notes.take(200))
                },
            )
        }
        return JSONObject().apply {
            put("date", today)
            put("tasks", arr)
        }.toString()
    }

    private suspend fun recentReflectionsJson(): String {
        val uid = authRepository.currentUserId()
        val rows = db.reflectionDao().getRecentSnapshot(userId = uid, limit = 6)
        val arr = JSONArray()
        rows.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("date", r.date)
                    put("summary", r.textInput.take(350))
                },
            )
        }
        return JSONObject().put("reflections", arr).toString()
    }
}
