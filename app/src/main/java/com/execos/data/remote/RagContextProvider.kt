package com.execos.data.remote

import com.execos.data.local.ExecOsDatabase
import com.execos.data.repo.AuthRepository
import com.execos.util.Dates
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight RAG: pulls bounded snapshots from Room and formats them as plain text
 * so the model can ground answers in the user's own journal (no vector DB).
 */
@Singleton
class RagContextProvider @Inject constructor(
    private val db: ExecOsDatabase,
    private val authRepository: AuthRepository,
) {
    suspend fun buildContextBlock(): String {
        val uid = authRepository.currentUserId()
        val today = Dates.todayIso()
        val tasks = db.taskDao().getTasksForDateSnapshot(userId = uid, date = today)
        val decisions = db.decisionDao().getRecentSnapshot(userId = uid, limit = 8)
        val reflections = db.reflectionDao().getRecentSnapshot(userId = uid, limit = 5)
        val strava = db.stravaActivityDao().getRecentSnapshot(userId = uid, limit = 8)

        return buildString {
            if (tasks.isNotEmpty()) {
                appendLine("Today's priorities ($today):")
                tasks.forEach { t ->
                    appendLine(
                        "- ${t.title} | impact ${t.impactScore} | done=${t.completed}" +
                            if (t.notes.isNotBlank()) " | notes: ${t.notes.take(120)}" else "",
                    )
                }
                appendLine()
            }
            if (decisions.isNotEmpty()) {
                appendLine("Recent decisions (newest first):")
                decisions.forEach { d ->
                    appendLine(
                        "- ${d.title} (${d.date}) conf=${d.confidence}/5 — " +
                            d.finalDecision.take(100).let { if (it.length < d.finalDecision.length) "$it…" else it },
                    )
                }
                appendLine()
            }
            if (reflections.isNotEmpty()) {
                appendLine("Recent reflections:")
                reflections.forEach { r ->
                    appendLine("- ${r.date}: ${r.textInput.take(200)}${if (r.textInput.length > 200) "…" else ""}")
                }
                appendLine()
            }
            if (strava.isNotEmpty()) {
                appendLine("Recent workouts (Strava):")
                strava.forEach { a ->
                    val mins = (a.movingTimeSec / 60.0).toInt()
                    val km = a.distanceMeters / 1000.0
                    appendLine("- ${a.type}: ${a.name.ifBlank { "(no name)" }} | ${mins}m | ${"%.1f".format(km)}km | ${a.startDate}")
                }
            }
        }.trim()
    }
}
