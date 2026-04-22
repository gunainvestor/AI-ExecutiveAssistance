package com.execos.daily

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.execos.data.model.ReflectionItem
import com.execos.data.remote.AiService
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.DailyFeedbackStore
import com.execos.data.repo.ReflectionRepository
import com.execos.data.repo.TaskRepository
import com.execos.data.usage.DeviceUsageRepository
import com.execos.util.Dates
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalTime

class DailyFeedbackWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyFeedbackEntryPoint {
        fun authRepository(): AuthRepository
        fun taskRepository(): TaskRepository
        fun reflectionRepository(): ReflectionRepository
        fun deviceUsageRepository(): DeviceUsageRepository
        fun aiService(): AiService
        fun dailyFeedbackStore(): DailyFeedbackStore
    }

    override suspend fun doWork(): Result {
        // Guard: if the job drifts too early, skip (we schedule "around 6pm" but don't want 2am runs).
        val hour = runCatching { LocalTime.now().hour }.getOrDefault(18)
        if (hour < 15) return Result.success()

        val ep = EntryPointAccessors.fromApplication(
            applicationContext,
            DailyFeedbackEntryPoint::class.java,
        )

        val uid = ep.authRepository().ensureSignedIn().getOrNull() ?: return Result.success()
        val today = Dates.todayIso()

        val tasks = runCatching { ep.taskRepository().observeTasksForDate(uid, today).first() }
            .getOrElse { emptyList() }

        val minutesToday = runCatching {
            if (!ep.deviceUsageRepository().hasUsageAccess()) 0
            else ep.deviceUsageRepository().getSocialAndBrowserUsage().getOrNull().orEmpty().sumOf { it.minutesToday }
        }.getOrDefault(0)

        val daySummary = buildString {
            appendLine("Date: $today")
            appendLine()
            appendLine("Planned priorities:")
            if (tasks.isEmpty()) {
                appendLine("- (none)")
            } else {
                tasks
                    .sortedWith(compareByDescending<com.execos.data.model.TaskItem> { it.impactScore }.thenBy { it.title })
                    .take(10)
                    .forEach { t ->
                        append("- ")
                        append(t.title)
                        append(" | impact ")
                        append(t.impactScore)
                        append(" | done=")
                        append(t.completed)
                        if (t.notes.isNotBlank()) {
                            append(" | notes: ")
                            append(t.notes.take(140))
                        }
                        appendLine()
                    }
            }
            appendLine()
            appendLine("Social + browsing minutes today: $minutesToday")
        }.trim()

        val ai = ep.aiService().endOfDayFeedback(daySummary = daySummary, minutesSocialAndBrowsingToday = minutesToday)
            .getOrNull()
            ?.trim()
            .orEmpty()

        if (ai.isBlank()) return Result.retry()

        runCatching {
            ep.reflectionRepository().saveReflection(
                uid,
                ReflectionItem(
                    textInput = "Auto end-of-day summary (planned priorities + usage).",
                    aiOutput = ai,
                    date = today,
                ),
            )
        }

        runCatching { ep.dailyFeedbackStore().setFeedback(date = today, text = ai) }
        runCatching { DailyFeedbackNotifier.show(applicationContext, ai) }

        return Result.success()
    }
}

